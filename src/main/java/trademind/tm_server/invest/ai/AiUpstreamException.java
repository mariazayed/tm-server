package trademind.tm_server.invest.ai;

import trademind.tm_server.ai.LlmException;

/**
 * The model call failed. Carries no provider text: the error body can echo request content, so it is
 * never logged or shown. Only the {@link LlmException.Kind} survives, which comes from the connection or
 * the HTTP status and lets the caller be told something useful (is Ollama running? is the model pulled?).
 */
public class AiUpstreamException extends RuntimeException {

    private final LlmException.Kind kind;

    public AiUpstreamException() {
        this(LlmException.Kind.OTHER);
    }

    public AiUpstreamException(LlmException.Kind kind) {
        super("The AI service did not answer");
        this.kind = kind;
    }

    public LlmException.Kind kind() {
        return kind;
    }

    /** A fixed sentence for the caller, or empty when nothing more is known. */
    public String hint() {
        return switch (kind) {
            case UNREACHABLE -> "Could not connect to the AI provider. If you use Ollama, check that it is running.";
            case TIMEOUT -> "The model took too long to answer. A local model can be slow: try again, or use a smaller model.";
            case MODEL_NOT_FOUND -> "The provider does not know the configured model. For Ollama, run \"ollama pull\" for it.";
            case REJECTED_KEY -> "The provider rejected the API key.";
            case RATE_LIMITED -> "The provider is rate limiting this server. Try again in a minute.";
            case OTHER -> "";
        };
    }
}
