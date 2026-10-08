package trademind.tm_server.invest.ai;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/** Test double for {@link AiUsageStore}: same counting rules, no database. */
class InMemoryAiUsageStore implements AiUsageStore {

    private final Map<String, long[]> rows = new TreeMap<>(); // key "date|FEATURE" -> {calls, in, out}
    boolean failOnRecord;

    private static String key(LocalDate day, AiFeature feature) {
        return day + "|" + feature.name();
    }

    @Override
    public synchronized Totals totals(LocalDate day) {
        Map<String, Long> byFeature = new TreeMap<>();
        long calls = 0, in = 0, out = 0;
        for (var e : rows.entrySet()) {
            if (!e.getKey().startsWith(day + "|")) continue;
            byFeature.put(e.getKey().substring(e.getKey().indexOf('|') + 1), e.getValue()[0]);
            calls += e.getValue()[0];
            in += e.getValue()[1];
            out += e.getValue()[2];
        }
        return new Totals(calls, in, out, byFeature);
    }

    @Override
    public synchronized void recordCall(LocalDate day, AiFeature feature) {
        if (failOnRecord) throw new IllegalStateException("store down");
        rows.computeIfAbsent(key(day, feature), k -> new long[3])[0]++;
    }

    @Override
    public synchronized void addTokens(LocalDate day, AiFeature feature, long inputTokens, long outputTokens) {
        long[] row = rows.computeIfAbsent(key(day, feature), k -> new long[3]);
        row[1] += inputTokens;
        row[2] += outputTokens;
    }
}
