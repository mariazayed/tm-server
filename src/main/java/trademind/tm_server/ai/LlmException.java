package trademind.tm_server.ai;

/** Unchecked wrapper for any LlmClient failure -- network error, non-2xx
 * response, or a response body that doesn't parse the way the client
 * expects. Lets AiSuggestionService catch one exception type regardless of
 * which provider is configured. */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
