package trademind.tm_server.invest.rulecheck;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Runs the deterministic consistency checks over a date range. Read-only and independent of the AI
 * switch: it needs no model and sends nothing anywhere, so it works with AI off. Findings are computed
 * on request and never stored.
 */
@Service
@Transactional(readOnly = true)
public class RuleCheckService {

    public static final int MAX_RANGE_DAYS = 92;
    /** Entries are read this far before the range, enough for every check to look back from its first day. */
    static final int LOOKBACK_DAYS = 45;
    /** Entries are read this far after the range, for the week a missed month still has to be explained. */
    static final int LOOKAHEAD_DAYS = 8;
    private static final int MAX_ROWS = 5_000;

    private static final List<RuleCheck> CHECKS = List.of(new WaitPeriodCheck(), new PlanChangeCheck(), new MissedMonthCheck());

    /** @param status RAN, or OFF with the reason in {@code note} */
    public record CheckInfo(String code, String label, String status, String note) {
    }

    public record Result(LocalDate from, LocalDate to, int waitHours, List<RuleFinding> findings, List<CheckInfo> checks) {
    }

    private final JdbcTemplate jdbc;
    private final InvestSettingsRepository settings;
    private final ContributionService contributions;
    private final Clock clock;

    public RuleCheckService(JdbcTemplate jdbc, InvestSettingsRepository settings, ContributionService contributions,
                            Clock clock) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.contributions = contributions;
        this.clock = clock;
    }

    /** @throws BadRequestException unless both dates are given, in order, and at most {@value #MAX_RANGE_DAYS} days apart */
    public static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new BadRequestException("Choose a from and a to date");
        if (to.isBefore(from)) throw new BadRequestException("The to date is before the from date");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BadRequestException("Choose a range of at most " + MAX_RANGE_DAYS + " days");
        }
    }

    public Result run(LocalDate from, LocalDate to) {
        validateRange(from, to);
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        Facts facts = new Facts(from, to, LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC), s.getRuleWaitHours(),
                s.getCurrency(), entries(from, to), changes(from, to), missedMonths());

        List<RuleFinding> findings = CHECKS.stream().flatMap(c -> c.run(facts).stream())
                .sorted(Comparator.comparing(RuleFinding::date).reversed().thenComparing(RuleFinding::check))
                .toList();
        List<CheckInfo> infos = CHECKS.stream().map(c -> {
            String off = c.offReason(facts);
            return new CheckInfo(c.code(), c.label(), off == null ? "RAN" : "OFF", off);
        }).toList();
        return new Result(from, to, s.getRuleWaitHours(), findings, infos);
    }

    private List<Facts.Entry> entries(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT e.id, e.created_at,
                       (SELECT coalesce(array_agg(lower(t.name)), '{}') FROM invest_journal_entry_tags jt
                          JOIN invest_tags t ON t.id = jt.tag_id AND t.deleted_at IS NULL
                         WHERE jt.entry_id = e.id) AS tags
                FROM invest_journal_entries e
                WHERE e.deleted_at IS NULL AND e.created_at >= ? AND e.created_at < ?
                ORDER BY e.created_at, e.id
                LIMIT ?""",
                (rs, i) -> new Facts.Entry(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(),
                        new HashSet<>(Arrays.asList((String[]) rs.getArray("tags").getArray()))),
                Timestamp.valueOf(from.minusDays(LOOKBACK_DAYS).atStartOfDay()),
                Timestamp.valueOf(to.plusDays(LOOKAHEAD_DAYS).atStartOfDay()), MAX_ROWS);
    }

    private List<Facts.PlanChange> changes(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT created_at, field_name, old_value, new_value FROM invest_settings_changes
                WHERE deleted_at IS NULL AND created_at >= ? AND created_at < ?
                ORDER BY created_at, id""",
                (rs, i) -> new Facts.PlanChange(rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("field_name"),
                        rs.getString("old_value"), rs.getString("new_value")),
                Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }

    private List<YearMonth> missedMonths() {
        return contributions.status().months().stream()
                .filter(m -> m.state() == ContributionStatusCalculator.MonthState.MISSED)
                .map(ContributionStatusCalculator.Month::month)
                .toList();
    }
}
