package trademind.tm_server.invest.ai;

/** A daily or per-minute cap was reached; nothing was sent to the model. */
public class AiLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public AiLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
