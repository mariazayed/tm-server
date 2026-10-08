package trademind.tm_server.ai;

/**
 * Per-call settings for {@link LlmClient#completeWithUsage}.
 *
 * @param maxTokens      upper bound on the reply length
 * @param temperature    null leaves the provider default
 * @param timeoutSeconds null uses {@code ai.provider.timeout-seconds}
 */
public record LlmOptions(int maxTokens, Double temperature, Integer timeoutSeconds) {

    /** What {@link LlmClient#complete} has always used. */
    public static final int DEFAULT_MAX_TOKENS = 1536;

    public LlmOptions {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (temperature != null && (temperature < 0 || temperature > 1)) {
            throw new IllegalArgumentException("temperature must be between 0 and 1");
        }
        if (timeoutSeconds != null && timeoutSeconds < 1) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
    }

    public static LlmOptions defaults() {
        return new LlmOptions(DEFAULT_MAX_TOKENS, null, null);
    }
}
