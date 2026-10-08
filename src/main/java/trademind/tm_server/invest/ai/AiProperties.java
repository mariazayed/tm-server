package trademind.tm_server.invest.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Switches and spending limits for the AI features. Off by default: nothing is sent to a model until
 * {@code ai.enabled=true} and a key and model are configured.
 *
 * @param enabled master switch; false makes every AI route answer 503 without touching the network
 * @param limits  caps that bound how much a caller can spend
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue Limits limits) {

    /**
     * @param dailyTotal     model calls per UTC day, all features together
     * @param perMinute      model calls in any rolling 60 seconds
     * @param maxPromptChars system plus user prompt, in characters, before a call is refused
     */
    public record Limits(
            @DefaultValue("60") int dailyTotal,
            @DefaultValue("6") int perMinute,
            @DefaultValue("20000") int maxPromptChars) {

        public Limits {
            if (dailyTotal < 1 || perMinute < 1 || maxPromptChars < 1) {
                throw new IllegalArgumentException("ai.limits values must be positive");
            }
        }
    }
}
