package trademind.tm_server.invest.ai;

/**
 * The model answered twice, and neither answer could be used as the JSON the feature needs. Carries no
 * detail: the reasons are in the logs by category, and the reply itself is never kept or shown.
 */
public class AiInvalidReplyException extends RuntimeException {
    public AiInvalidReplyException() {
        super("The AI answered in a form that could not be used");
    }
}
