package trademind.tm_server.invest.checkin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.PriceQuote;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator;
import trademind.tm_server.invest.performance.PerformanceCalculator;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;
import trademind.tm_server.invest.rulecheck.RuleCheckService;
import trademind.tm_server.invest.rulecheck.RuleFinding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Computes {@link CheckinFacts} for one month and reads that month's journal entries. Read-only, and it
 * needs no model: the facts are plain arithmetic over the owner's own records, so they are available with
 * AI switched off.
 */
@Service
@Transactional(readOnly = true)
public class CheckinFactsService {

    public static final int MAX_TAGS = 5;
    public static final int MAX_RULE_NOTES = 5;
    private static final int MAX_ENTRIES = 500;
    private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    /** A journal entry of the month, with the text the summary may draw on. */
    public record MonthEntry(long id, LocalDateTime createdAt, String type, Short mood, List<String> tags, String body) {
    }

    /** @param entries every live entry of the month, oldest first */
    public record Loaded(CheckinFacts facts, List<MonthEntry> entries) {
    }

    private final ContributionRepository contributions;
    private final PriceQuoteRepository prices;
    private final InvestSettingsRepository settings;
    private final ContributionService contributionService;
    private final RuleCheckService ruleChecks;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CheckinFactsService(ContributionRepository contributions, PriceQuoteRepository prices,
                               InvestSettingsRepository settings, ContributionService contributionService,
                               RuleCheckService ruleChecks, JdbcTemplate jdbc, Clock clock) {
        this.contributions = contributions;
        this.prices = prices;
        this.settings = settings;
        this.contributionService = contributionService;
        this.ruleChecks = ruleChecks;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** @throws BadRequestException unless {@code month} is YYYY-MM, not before the plan started, and not in the future */
    public YearMonth parse(String month) {
        if (month == null || !MONTH.matcher(month).matches()) {
            throw new BadRequestException("Choose a month like 2026-09");
        }
        YearMonth ym = YearMonth.parse(month);
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        if (ym.isBefore(YearMonth.from(s.getStartDate()))) {
            throw new BadRequestException("That month is before your plan started");
        }
        if (ym.isAfter(YearMonth.from(today()))) {
            throw new BadRequestException("That month has not started yet");
        }
        return ym;
    }

    public Loaded load(YearMonth month) {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        LocalDate asOf = end.isAfter(today()) ? today() : end; // the current month is "so far"

        List<Contribution> monthBuys = contributions.findByDeletedAtIsNullAndContributedOnBetweenOrderByContributedOnAsc(start, end);
        List<CheckinFacts.Buy> buys = monthBuys.stream()
                .map(c -> new CheckinFacts.Buy(c.getContributedOn(), c.getAmountCents(), c.getShares(), c.getPrice(), c.getFeeCents()))
                .toList();
        long invested = buys.stream().mapToLong(CheckinFacts.Buy::amountCents).sum();
        long fees = buys.stream().mapToLong(CheckinFacts.Buy::feeCents).sum();
        BigDecimal shares = buys.stream().map(CheckinFacts.Buy::shares).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = shares.signum() > 0
                ? BigDecimal.valueOf(invested).movePointLeft(2).divide(shares, 4, RoundingMode.HALF_UP) : null;
        LocalDate due = month.atDay(s.getBuyDay());
        LocalDate firstBuy = buys.isEmpty() ? null : buys.get(0).date();
        Integer daysAfterDue = firstBuy == null ? null : (int) ChronoUnit.DAYS.between(due, firstBuy);
        Long linkId = monthBuys.isEmpty() ? null : monthBuys.get(monthBuys.size() - 1).getId();

        ContributionStatusCalculator.Result status = contributionService.status();
        String state = status.months().stream().filter(m -> m.month().equals(month)).findFirst()
                .map(m -> m.state().name()).orElse("NONE");

        List<MonthEntry> entries = entries(start, end);
        List<Integer> moods = entries.stream().filter(e -> e.mood() != null).map(e -> e.mood().intValue()).toList();
        BigDecimal moodAverage = moods.isEmpty() ? null
                : BigDecimal.valueOf(moods.stream().mapToInt(Integer::intValue).sum())
                        .divide(BigDecimal.valueOf(moods.size()), 1, RoundingMode.HALF_UP);

        List<String> ruleNotes = ruleChecks.run(start, end).findings().stream()
                .filter(f -> RuleFinding.DETERMINISTIC.equals(f.source())).map(RuleFinding::message)
                .limit(MAX_RULE_NOTES).toList();

        CheckinFacts facts = new CheckinFacts(month.toString(), s.getCurrency(), s.getMonthlyAmountCents(), s.getBuyDay(), due,
                state, buys, invested, fees, shares, average, firstBuy, daysAfterDue, status.currentStreak(),
                status.longestStreak(), status.missedCount(), monthEnd(s.getTicker(), asOf), entries.size(), moods,
                moodAverage, tagCounts(entries), ruleNotes, linkId);
        return new Loaded(facts, entries);
    }

    /** Value at the end of the month from cached market prices; null when there is none on or before that day. */
    private CheckinFacts.MonthEndValue monthEnd(String ticker, LocalDate asOf) {
        List<PerformanceCalculator.Buy> upTo = contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                .filter(c -> !c.getContributedOn().isAfter(asOf))
                .map(c -> new PerformanceCalculator.Buy(c.getContributedOn(), c.getAmountCents(), c.getShares(), c.getPrice(), c.getFeeCents()))
                .toList();
        if (upTo.isEmpty()) return null;
        TreeMap<LocalDate, BigDecimal> closes = new TreeMap<>();
        for (PriceQuote q : prices.findByTickerAndPriceDateBetweenOrderByPriceDateAsc(ticker, LocalDate.of(1900, 1, 1), asOf)) {
            closes.put(q.getPriceDate(), q.getClose());
        }
        if (closes.isEmpty()) return null;
        PerformanceCalculator.Result r = PerformanceCalculator.calculate(upTo, closes, asOf);
        return new CheckinFacts.MonthEndValue(r.priceAsOf(), r.investedCents(), r.valueCents(), r.gainCents(), r.gainPercent());
    }

    private static List<CheckinFacts.TagCount> tagCounts(List<MonthEntry> entries) {
        Map<String, Integer> counts = new TreeMap<>();
        for (MonthEntry e : entries) for (String t : e.tags()) counts.merge(t, 1, Integer::sum);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_TAGS).map(e -> new CheckinFacts.TagCount(e.getKey(), e.getValue())).toList();
    }

    private List<MonthEntry> entries(LocalDate start, LocalDate end) {
        return new ArrayList<>(jdbc.query("""
                SELECT e.id, e.created_at, e.entry_type, e.mood, e.body,
                       (SELECT coalesce(array_agg(lower(t.name) ORDER BY lower(t.name)), '{}') FROM invest_journal_entry_tags jt
                          JOIN invest_tags t ON t.id = jt.tag_id AND t.deleted_at IS NULL
                         WHERE jt.entry_id = e.id) AS tags
                FROM invest_journal_entries e
                WHERE e.deleted_at IS NULL AND e.created_at >= ? AND e.created_at < ?
                ORDER BY e.created_at, e.id
                LIMIT ?""",
                (rs, i) -> {
                    short mood = rs.getShort("mood");
                    Short moodValue = rs.wasNull() ? null : mood; // read right after the column it describes
                    return new MonthEntry(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("entry_type"), moodValue, List.of((String[]) rs.getArray("tags").getArray()),
                            rs.getString("body"));
                },
                Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.plusDays(1).atStartOfDay()), MAX_ENTRIES));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
