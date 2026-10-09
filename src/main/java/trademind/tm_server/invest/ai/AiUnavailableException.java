package trademind.tm_server.invest.ai;

/** AI is switched off or not configured; the message says what to set and is safe to show. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String reason) {
        super(reason);
    }
}
