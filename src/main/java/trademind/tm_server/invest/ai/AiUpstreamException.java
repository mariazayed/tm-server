package trademind.tm_server.invest.ai;

/**
 * The model call failed. Deliberately carries no detail: the provider's error body can echo request
 * content, so only the exception class is ever logged and nothing is shown to the caller.
 */
public class AiUpstreamException extends RuntimeException {
    public AiUpstreamException() {
        super("The AI service did not answer");
    }
}
