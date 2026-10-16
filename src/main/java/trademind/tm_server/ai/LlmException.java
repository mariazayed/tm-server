package trademind.tm_server.ai;

public class LlmException extends RuntimeException {

    /**
     * Why a call failed, in words that are safe to show: it comes from the connection or the HTTP status,
     * never from the provider's response body (which can echo what was sent).
     */
    public enum Kind { UNREACHABLE, TIMEOUT, MODEL_NOT_FOUND, REJECTED_KEY, RATE_LIMITED, OTHER }

    private final Kind kind;

    public LlmException(String message) {
        this(message, Kind.OTHER);
    }

    public LlmException(String message, Kind kind) {
        super(message);
        this.kind = kind;
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
        this.kind = Kind.OTHER;
    }

    public Kind kind() {
        return kind;
    }
}
