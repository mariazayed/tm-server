package trademind.tm_server.invest.ai;

import trademind.tm_server.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trademind.tm_server.ai.LlmClient;
import trademind.tm_server.ai.LlmResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * The one door to the model for every investing AI feature. It checks the master switch and the
 * configuration, enforces the spending caps, makes the call and counts it. It never writes owner data,
 * and it logs sizes and timings only: prompts, replies and provider error bodies can contain what the
 * owner wrote, so none of them reach a log or a response.
 *
 * <p>The day for the daily cap is the UTC day, so it resets at 00:00 UTC.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    /** Today's spending, for the settings screen. */
    public record Usage(LocalDate date, long callsToday, int dailyLimit, int perMinuteLimit,
                        long inputTokens, long outputTokens, Map<String, Long> callsByFeature) {
    }

    public record UsageDay(LocalDate date, long calls) {
    }

    /** Calls per day against the daily cap, for the usage chart. */
    public record UsageHistory(int dailyLimit, java.util.List<UsageDay> days) {
    }

    private final LlmClient llm;
    private final AiUsageStore usageStore;
    private final AiProperties properties;
    private final Clock clock;
    private final String apiKey;
    private final String model;
    private final AiRateLimiter minuteLimiter;
    private final Object reserveLock = new Object();

    public AiService(LlmClient llm, AiUsageStore usageStore, AiProperties properties, Clock clock,
                     String apiKey, String model) {
        this(llm, usageStore, properties, clock, apiKey, "anthropic", model);
    }

    @Autowired
    public AiService(LlmClient llm, AiUsageStore usageStore, AiProperties properties, Clock clock,
                     @Value("${ai.provider.api-key:}") String apiKey,
                     @Value("${ai.provider.type:anthropic}") String providerType,
                     @Value("${ai.provider.model:}") String model) {
        this.llm = llm;
        this.usageStore = usageStore;
        this.properties = properties;
        this.clock = clock;
        // A local Ollama needs no key; the placeholder only lets the "configured" check pass.
        this.apiKey = (apiKey == null || apiKey.isBlank()) && "ollama".equals(providerType) ? "ollama" : apiKey;
        this.model = model;
        this.minuteLimiter = new AiRateLimiter(properties.limits().perMinute(), Duration.ofMinutes(1), clock);
    }

    public AiController.Status status() {
        return AiController.status(properties.enabled(), apiKey, model);
    }

    /** @throws AiUnavailableException when AI is switched off or not configured */
    public void requireAvailable() {
        AiController.Status status = status();
        if (!status.configured()) {
            throw new AiUnavailableException(status.reason());
        }
    }

    /**
     * Runs one model call.
     *
     * @throws AiUnavailableException  switched off or not configured (nothing was sent)
     * @throws AiLimitExceededException a cap was reached (nothing was sent)
     * @throws BadRequestException the prompt is over {@code ai.limits.max-prompt-chars} (nothing was sent)
     * @throws AiUpstreamException      the call failed; no detail is carried
     */
    public LlmResult call(AiFeature feature, String systemPrompt, String userPrompt) {
        requireAvailable();
        int chars = systemPrompt.length() + userPrompt.length();
        int max = properties.limits().maxPromptChars();
        if (chars > max) {
            throw new BadRequestException("The prompt is too large to send (" + chars + " of " + max + " characters)");
        }
        LocalDate day = today();
        reserve(day, feature);

        long started = System.nanoTime();
        LlmResult result;
        try {
            result = llm.completeWithUsage(systemPrompt, userPrompt, feature.options());
        } catch (RuntimeException e) {
            // Class name only: the message can carry the provider's response body.
            log.warn("ai call failed feature={} promptChars={} ms={} error={}", feature, chars, millisSince(started),
                    e.getClass().getSimpleName());
            throw new AiUpstreamException();
        }
        if (result.inputTokens() != null || result.outputTokens() != null) {
            try {
                usageStore.addTokens(day, feature, orZero(result.inputTokens()), orZero(result.outputTokens()));
            } catch (RuntimeException e) {
                log.warn("ai token count not saved feature={} error={}", feature, e.getClass().getSimpleName());
            }
        }
        log.info("ai call feature={} promptChars={} replyChars={} ms={} inputTokens={} outputTokens={} stop={}",
                feature, chars, result.text().length(), millisSince(started), result.inputTokens(),
                result.outputTokens(), result.stopReason());
        return result;
    }

    /** Calls per day for the last {@code days} days, today included; days with no calls are filled in as zero. */
    public UsageHistory history(int days) {
        LocalDate to = today();
        LocalDate from = to.minusDays(days - 1L);
        java.util.Map<LocalDate, Long> byDay = new java.util.HashMap<>();
        for (AiUsageStore.DayCalls d : usageStore.history(from, to)) byDay.put(d.date(), d.calls());
        java.util.List<UsageDay> out = new java.util.ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) out.add(new UsageDay(d, byDay.getOrDefault(d, 0L)));
        return new UsageHistory(properties.limits().dailyTotal(), out);
    }

    public Usage usage() {
        LocalDate day = today();
        AiUsageStore.Totals totals = usageStore.totals(day);
        return new Usage(day, totals.calls(), properties.limits().dailyTotal(), properties.limits().perMinute(),
                totals.inputTokens(), totals.outputTokens(), totals.callsByFeature());
    }

    /** Checks both caps and counts the call, under one lock so parallel requests cannot overshoot. */
    private void reserve(LocalDate day, AiFeature feature) {
        synchronized (reserveLock) {
            long calls = usageStore.totals(day).calls();
            int daily = properties.limits().dailyTotal();
            if (calls >= daily) {
                long untilReset = Duration.between(clock.instant(),
                        day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()).toSeconds();
                throw new AiLimitExceededException(
                        "The daily AI limit (" + daily + " calls) is used up. It resets at midnight UTC.",
                        Math.max(1, untilReset));
            }
            long wait = minuteLimiter.retryAfterSeconds();
            if (wait > 0) {
                throw new AiLimitExceededException("Too many AI calls in the last minute. Try again shortly.", wait);
            }
            minuteLimiter.record();
            usageStore.recordCall(day, feature);
        }
    }

    private LocalDate today() {
        return LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    }

    private static long orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
