package trademind.tm_server.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The OpenAI-compatible client against a local stand-in: request shape, parsing, and model fallback. */
class OpenAiCompatibleLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private final List<JsonNode> bodies = new CopyOnWriteArrayList<>();
    private final List<String> auth = new CopyOnWriteArrayList<>();
    /** Status per call, in order; the last one repeats. */
    private volatile List<Integer> statuses = List.of(200);
    private volatile String okBody = """
            {"choices":[{"message":{"content":"Hi"},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":11,"completion_tokens":3}}""";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            bodies.add(mapper.readTree(exchange.getRequestBody().readAllBytes()));
            auth.add(exchange.getRequestHeaders().getFirst("Authorization"));
            int n = bodies.size() - 1;
            int status = statuses.get(Math.min(n, statuses.size() - 1));
            byte[] bytes = (status == 200 ? okBody : "{\"error\":\"nope\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private OpenAiCompatibleLlmClient client(String fallbacks) {
        OpenAiCompatibleLlmClient c = new OpenAiCompatibleLlmClient();
        ReflectionTestUtils.setField(c, "apiKey", "k");
        ReflectionTestUtils.setField(c, "model", "main");
        ReflectionTestUtils.setField(c, "fallbackModels", fallbacks);
        ReflectionTestUtils.setField(c, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        ReflectionTestUtils.setField(c, "timeoutSeconds", 5);
        return c;
    }

    @Test
    void sendsSystemAndUserMessagesAndReadsUsage() {
        LlmResult r = client("").completeWithUsage("sys", "usr", new LlmOptions(300, 0.2, null));
        assertEquals("Hi", r.text());
        assertEquals(11, r.inputTokens());
        assertEquals(3, r.outputTokens());
        JsonNode b = bodies.get(0);
        assertEquals("main", b.get("model").asText());
        assertEquals(300, b.get("max_tokens").asInt());
        assertEquals("system", b.get("messages").get(0).get("role").asText());
        assertEquals("user", b.get("messages").get(1).get("role").asText());
        assertEquals("Bearer k", auth.get(0));
    }

    @Test
    void lengthFinishCountsAsTruncated() {
        okBody = "{\"choices\":[{\"message\":{\"content\":\"cut\"},\"finish_reason\":\"length\"}]}";
        assertTrue(client("").completeWithUsage("s", "u", LlmOptions.defaults()).truncated());
    }

    @Test
    void rateLimitFallsBackToTheNextModel() {
        statuses = List.of(429, 200);
        LlmResult r = client(" backup , main, ").completeWithUsage("s", "u", LlmOptions.defaults());
        assertEquals("Hi", r.text());
        List<String> used = new ArrayList<>();
        bodies.forEach(b -> used.add(b.get("model").asText()));
        assertEquals(List.of("main", "backup"), used);
    }

    @Test
    void aClientErrorIsNotRetried() {
        statuses = List.of(401);
        assertThrows(LlmException.class, () -> client("backup").completeWithUsage("s", "u", LlmOptions.defaults()));
        assertEquals(1, bodies.size());
    }

    @Test
    void failsWhenEveryModelIsRateLimited() {
        statuses = List.of(429);
        assertThrows(LlmException.class, () -> client("backup").completeWithUsage("s", "u", LlmOptions.defaults()));
        assertEquals(2, bodies.size());
    }

    @Test
    void needsKeyModelAndBaseUrl() {
        OpenAiCompatibleLlmClient c = new OpenAiCompatibleLlmClient();
        assertThrows(LlmException.class, () -> c.complete("s", "u"));
    }
}
