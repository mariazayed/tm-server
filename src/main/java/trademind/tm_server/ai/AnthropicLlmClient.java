package trademind.tm_server.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to Anthropic's Messages API (api.anthropic.com/v1/messages) using
 * the JDK's built-in HttpClient -- deliberately no extra SDK dependency, to
 * avoid adding a new Maven coordinate that can't be verified to resolve in
 * this build environment (see the repo's other phase notes on the Maven
 * network restriction here).
 *
 * Configuration (see application-local.properties.example):
 *   ai.provider.api-key   -- required to actually call out; blank disables
 *                            calls (throws LlmException with a clear message)
 *   ai.provider.model     -- required, no hardcoded default. Set this to
 *                            whatever current Claude model your API key has
 *                            access to -- see docs.claude.com for the
 *                            current list; this code intentionally doesn't
 *                            guess a model id that could go stale.
 *   ai.provider.base-url  -- defaults to https://api.anthropic.com
 *   ai.provider.timeout-seconds -- defaults to 60
 */
@Service
public class AnthropicLlmClient implements LlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${ai.provider.api-key:}")
    private String apiKey;

    @Value("${ai.provider.model:}")
    private String model;

    @Value("${ai.provider.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${ai.provider.timeout-seconds:60}")
    private int timeoutSeconds;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return completeWithUsage(systemPrompt, userPrompt, LlmOptions.defaults()).text();
    }

    @Override
    public LlmResult completeWithUsage(String systemPrompt, String userPrompt, LlmOptions options) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(
                    "ai.provider.api-key is not set -- add it to application-local.properties " +
                    "to enable AI suggestions."
            );
        }
        if (model == null || model.isBlank()) {
            throw new LlmException(
                    "ai.provider.model is not set -- set it to a current Claude model id " +
                    "(see docs.claude.com/en/docs/about-claude/models) in application-local.properties."
            );
        }

        String requestBody = buildRequestBody(systemPrompt, userPrompt, options);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .timeout(Duration.ofSeconds(options.timeoutSeconds() != null ? options.timeoutSeconds() : timeoutSeconds))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new LlmException("Request to Anthropic API failed: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new LlmException(
                    "Anthropic API returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        return parse(response.body());
    }

    private String buildRequestBody(String systemPrompt, String userPrompt, LlmOptions options) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", options.maxTokens());
        if (options.temperature() != null) {
            root.put("temperature", options.temperature());
        }
        root.put("system", systemPrompt);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // Should be unreachable -- we built the tree ourselves with plain
            // strings/ints -- but fail loudly rather than send a broken body.
            throw new LlmException("Failed to serialize Anthropic API request: " + e.getMessage(), e);
        }
    }

    private LlmResult parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content");
            if (content.isArray()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText());
                    }
                }
                if (!text.isEmpty()) {
                    JsonNode usage = root.path("usage");
                    return new LlmResult(text.toString(),
                            usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null,
                            usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null,
                            root.hasNonNull("stop_reason") ? root.get("stop_reason").asText() : null);
                }
            }
            throw new LlmException("Anthropic API response had no text content: " + responseBody);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to parse Anthropic API response: " + e.getMessage() +
                    " -- body: " + responseBody, e);
        }
    }
}
