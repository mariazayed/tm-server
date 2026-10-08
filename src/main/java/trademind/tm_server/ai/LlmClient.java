package trademind.tm_server.ai;

/**
 * A minimal seam between AiSuggestionService and whatever LLM provider is
 * actually configured (see AnthropicLlmClient). Kept this narrow so the
 * suggestion logic (AiSuggestionService), prompt construction
 * (AiSuggestionPromptBuilder) and response parsing
 * (AiSuggestionResponseParser) are all testable without ever making a real
 * network call -- only this interface's real implementation touches the
 * network.
 */
public interface LlmClient {

    /**
     * Sends one request and returns the model's raw text reply. Implementations
     * should throw LlmException on any failure (network, non-2xx, malformed
     * response) rather than returning null/empty -- callers need to
     * distinguish "the model said nothing useful" from "the call failed".
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * Like {@link #complete} but with per-call options and the provider's usage numbers. The default
     * keeps existing implementations and test doubles working: it ignores the options and reports no usage.
     */
    default LlmResult completeWithUsage(String systemPrompt, String userPrompt, LlmOptions options) {
        return new LlmResult(complete(systemPrompt, userPrompt), null, null, null);
    }
}
