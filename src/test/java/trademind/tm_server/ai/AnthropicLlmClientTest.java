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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real client against a local stand-in for the Messages API: request shape and response parsing. */
class AnthropicLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<JsonNode> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastKeyHeader = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            lastBody.set(mapper.readTree(exchange.getRequestBody().readAllBytes()));
            lastKeyHeader.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
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

    private AnthropicLlmClient client() {
        AnthropicLlmClient c = new AnthropicLlmClient();
        ReflectionTestUtils.setField(c, "apiKey", "test-key");
        ReflectionTestUtils.setField(c, "model", "test-model");
        ReflectionTestUtils.setField(c, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(c, "timeoutSeconds", 5);
        return c;
    }

    @Test
    void readsTextUsageAndStopReason() {
        response = """
                {"content":[{"type":"text","text":"Hello "},{"type":"text","text":"there"}],
                 "stop_reason":"end_turn","usage":{"input_tokens":42,"output_tokens":7}}""";
        LlmResult r = client().completeWithUsage("sys", "usr", new LlmOptions(300, 0.2, null));
        assertEquals("Hello there", r.text());
        assertEquals(42, r.inputTokens());
        assertEquals(7, r.outputTokens());
        assertEquals("end_turn", r.stopReason());
        assertFalse(r.truncated());
        assertEquals("test-key", lastKeyHeader.get());
    }

    @Test
    void sendsTheOptionsAndOmitsTemperatureWhenUnset() {
        response = "{\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}";
        client().completeWithUsage("sys", "usr", new LlmOptions(300, 0.2, null));
        JsonNode body = lastBody.get();
        assertEquals("test-model", body.get("model").asText());
        assertEquals(300, body.get("max_tokens").asInt());
        assertEquals(0.2, body.get("temperature").asDouble());
        assertEquals("sys", body.get("system").asText());
        assertEquals("usr", body.get("messages").get(0).get("content").asText());

        client().completeWithUsage("sys", "usr", new LlmOptions(300, null, null));
        assertFalse(lastBody.get().has("temperature"));
    }

    @Test
    void missingUsageAndStopReasonAreNull() {
        response = "{\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}";
        LlmResult r = client().completeWithUsage("s", "u", LlmOptions.defaults());
        assertNull(r.inputTokens());
        assertNull(r.outputTokens());
        assertNull(r.stopReason());
    }

    @Test
    void aMaxTokensStopIsReportedAsTruncated() {
        response = "{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"a\\\":\"}],\"stop_reason\":\"max_tokens\"}";
        assertTrue(client().completeWithUsage("s", "u", LlmOptions.defaults()).truncated());
    }

    @Test
    void theOldCompleteMethodKeepsItsDefaultsAndReturnsJustTheText() {
        response = "{\"content\":[{\"type\":\"text\",\"text\":\"plain\"}],\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}";
        assertEquals("plain", client().complete("s", "u"));
        assertEquals(1536, lastBody.get().get("max_tokens").asInt());
        assertFalse(lastBody.get().has("temperature"));
    }

    @Test
    void aNon2xxAnswerIsAnLlmException() {
        status = 429;
        response = "{\"error\":\"rate limited\"}";
        assertThrows(LlmException.class, () -> client().completeWithUsage("s", "u", LlmOptions.defaults()));
    }

    @Test
    void aReplyWithoutTextIsAnLlmException() {
        response = "{\"content\":[]}";
        assertThrows(LlmException.class, () -> client().completeWithUsage("s", "u", LlmOptions.defaults()));
    }

    @Test
    void optionsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new LlmOptions(0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new LlmOptions(10, 1.5, null));
        assertThrows(IllegalArgumentException.class, () -> new LlmOptions(10, null, 0));
    }
}
