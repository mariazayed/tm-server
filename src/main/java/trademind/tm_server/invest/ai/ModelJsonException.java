package trademind.tm_server.invest.ai;

/**
 * A model reply that could not be used as the expected JSON. The message is a short, fixed-vocabulary
 * description such as "the field 'severity' has a wrong type or a value that is not allowed". It is
 * built from the expected schema's own field names and never contains text taken from the reply, so it
 * is safe to put in a repair prompt and in a log line.
 */
public class ModelJsonException extends RuntimeException {

    private static final int MAX_REASON_CHARS = 200;

    public ModelJsonException(String reason) {
        super(clean(reason));
    }

    /** One line, bounded, so a caller-supplied reason cannot bloat or reshape a prompt. */
    private static String clean(String reason) {
        String flat = reason == null ? "the reply was not usable" : reason.replaceAll("\\s+", " ").strip();
        return flat.length() <= MAX_REASON_CHARS ? flat : flat.substring(0, MAX_REASON_CHARS - 1) + "…";
    }
}
