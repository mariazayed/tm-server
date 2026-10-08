package trademind.tm_server.ai;

/**
 * A model reply plus what the provider reported about it. Token counts and the stop reason are null
 * when the client could not tell (for example a test double).
 */
public record LlmResult(String text, Integer inputTokens, Integer outputTokens, String stopReason) {

    /** True when the reply was cut off by the token limit, so it may be incomplete. */
    public boolean truncated() {
        return "max_tokens".equals(stopReason);
    }
}
