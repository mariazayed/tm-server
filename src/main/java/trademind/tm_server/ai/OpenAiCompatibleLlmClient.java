package trademind.tm_server.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to any server that speaks the OpenAI "chat completions" API, which is how the free options
 * are reached: Google Gemini's free tier, Groq, OpenRouter's {@code :free} models, and a local Ollama or
 * LM Studio. Chosen with {@code ai.provider.type=openai}; the default stays Anthropic.
 *
 * <pre>
 *   ai.provider.type        = openai
 *   ai.provider.base-url    = the API root up to and including its version, e.g.
 *                             https://generativelanguage.googleapis.com/v1beta/openai   (Gemini)
 *                             https://api.groq.com/openai/v1                            (Groq)
 *                             https://openrouter.ai/api/v1                              (OpenRouter)
 *                             http://localhost:11434/v1                                 (Ollama)
 *   ai.provider.api-key     = the provider's key; Ollama ignores it but a value is still required (any word)
 *   ai.provider.model       = the model id
 *   ai.provider.fallback-models = optional, comma separated; tried in order when the model is rate limited
 *                             (HTTP 429) or the provider fails (5xx), which free tiers do often
 * </pre>
 *
 * Uses the JDK HttpClient, like {@link AnthropicLlmClient}, so there is no SDK dependency.
 */
@Service
@ConditionalOnProperty(name = "ai.provider.type", havingValue = "openai")
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${ai.provider.api-key:}")
    private String apiKey;

    @Value("${ai.provider.model:}")
    private String model;

    @Value("${ai.provider.fallback-models:}")
    private String fallbackModels;

    @Value("${ai.provider.base-url:}")
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
            throw new LlmException("ai.provider.api-key is not set (any word works for a local Ollama).");
        }
        if (model == null || model.isBlank()) {
            throw new LlmException("ai.provider.model is not set.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new LlmException("ai.provider.base-url is not set for ai.provider.type=openai.");
        }
        LlmException last = null;
        for (String candidate : models()) {
            try {
                return callOnce(candidate, systemPrompt, userPrompt, options);
            } catch (Retryable e) {
                last = new LlmException(e.getMessage());
            }
        }
        throw last;
    }

    /** The model followed by its fallbacks, blanks and repeats dropped. */
    List<String> models() {
        List<String> out = new ArrayList<>();
        out.add(model.strip());
        if (fallbackModels != null) {
            for (String m : fallbackModels.split(",")) {
                String t = m.strip();
                if (!t.isEmpty() && !out.contains(t)) out.add(t);
            }
        }
        return out;
    }

    /** Rate limited or a provider-side failure: worth trying the next model. */
    private static final class Retryable extends RuntimeException {
        Retryable(String message) {
            super(message);
        }
    }

    private LlmResult callOnce(String useModel, String systemPrompt, String userPrompt, LlmOptions options) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                .timeout(Duration.ofSeconds(options.timeoutSeconds() != null ? options.timeoutSeconds() : timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(useModel, systemPrompt, userPrompt, options)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new Retryable("Request to the AI provider failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Request to the AI provider was interrupted", e);
        }
        int code = response.statusCode();
        if (code == 429 || code >= 500) {
            throw new Retryable("AI provider returned HTTP " + code + ": " + response.body());
        }
        if (code < 200 || code >= 300) {
            throw new LlmException("AI provider returned HTTP " + code + ": " + response.body());
        }
        return parse(response.body());
    }

    private String body(String useModel, String systemPrompt, String userPrompt, LlmOptions options) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", useModel);
        root.put("max_tokens", options.maxTokens());
        if (options.temperature() != null) {
            root.put("temperature", options.temperature());
        }
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("Failed to serialize the AI request: " + e.getMessage(), e);
        }
    }

    private LlmResult parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").path(0);
            String text = choice.path("message").path("content").asText("");
            if (text.isBlank()) {
                throw new LlmException("AI provider response had no text content");
            }
            JsonNode usage = root.path("usage");
            String finish = choice.hasNonNull("finish_reason") ? choice.get("finish_reason").asText() : null;
            // Callers know one word for "cut off at the limit".
            if ("length".equals(finish)) finish = "max_tokens";
            return new LlmResult(text,
                    usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null,
                    usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null,
                    finish);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to parse the AI provider response: " + e.getMessage(), e);
        }
    }
}
