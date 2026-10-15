package trademind.tm_server.invest.ai;

import java.time.LocalDate;
import java.util.Map;

/** Persistent per-day call and token counts, so a restart does not hand out a fresh daily budget. */
public interface AiUsageStore {

    /** @param callsByFeature calls today per {@link AiFeature} name */
    record Totals(long calls, long inputTokens, long outputTokens, Map<String, Long> callsByFeature) {
    }

    Totals totals(LocalDate day);

    /** Calls per day from {@code from} to {@code to}, both included, oldest first; days with no calls are omitted. */
    java.util.List<DayCalls> history(LocalDate from, LocalDate to);

    record DayCalls(LocalDate date, long calls) {
    }

    /** Counts one call before it is made, so concurrent or failing calls cannot slip past the cap. */
    void recordCall(LocalDate day, AiFeature feature);

    void addTokens(LocalDate day, AiFeature feature, long inputTokens, long outputTokens);
}
