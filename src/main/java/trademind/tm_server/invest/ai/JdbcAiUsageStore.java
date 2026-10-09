package trademind.tm_server.invest.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class JdbcAiUsageStore implements AiUsageStore {

    private final JdbcTemplate jdbc;

    public JdbcAiUsageStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Totals totals(LocalDate day) {
        Map<String, Long> byFeature = new LinkedHashMap<>();
        long[] sums = new long[3];
        jdbc.query("SELECT feature, calls, input_tokens, output_tokens FROM invest_ai_usage "
                + "WHERE usage_date = ? ORDER BY feature", rs -> {
            byFeature.put(rs.getString("feature"), rs.getLong("calls"));
            sums[0] += rs.getLong("calls");
            sums[1] += rs.getLong("input_tokens");
            sums[2] += rs.getLong("output_tokens");
        }, Date.valueOf(day));
        return new Totals(sums[0], sums[1], sums[2], byFeature);
    }

    @Override
    public void recordCall(LocalDate day, AiFeature feature) {
        jdbc.update("INSERT INTO invest_ai_usage (usage_date, feature, calls) VALUES (?, ?, 1) "
                + "ON CONFLICT (usage_date, feature) DO UPDATE "
                + "SET calls = invest_ai_usage.calls + 1, updated_at = now()", Date.valueOf(day), feature.name());
    }

    @Override
    public void addTokens(LocalDate day, AiFeature feature, long inputTokens, long outputTokens) {
        jdbc.update("UPDATE invest_ai_usage SET input_tokens = input_tokens + ?, output_tokens = output_tokens + ?, "
                + "updated_at = now() WHERE usage_date = ? AND feature = ?",
                inputTokens, outputTokens, Date.valueOf(day), feature.name());
    }
}
