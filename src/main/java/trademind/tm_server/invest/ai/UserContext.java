package trademind.tm_server.invest.ai;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything an AI feature is allowed to know about the owner, in one compact object. It is built from
 * the same services the app screens use, so the AI sees the same numbers the owner sees.
 *
 * <p>Deliberately left out: the reminder email address, ids that mean nothing to a reader, tokens,
 * secrets, and anything older or longer than the caps in {@link UserContextService}.
 */
public record UserContext(
        Instant generatedAt,
        Plan plan,
        List<String> rules,
        Portfolio portfolio,
        Consistency consistency,
        List<Buy> recentBuys,
        List<Entry> recentJournal,
        List<Note> recentNotes,
        Cards flashcards,
        boolean truncated) {

    public record Plan(long monthlyAmountCents, String currency, int buyDay, String ticker, LocalDate startDate) {
    }

    /** @param pricesAreMarketData false when the value falls back to your own last purchase price */
    public record Portfolio(long investedCents, long valueCents, long gainCents, BigDecimal gainPercent,
                            BigDecimal shares, BigDecimal averageCost, LocalDate priceAsOf,
                            boolean pricesAreMarketData, boolean priceIsStale) {
    }

    /** @param thisMonth DONE, PENDING, OVERDUE, MISSED or NONE when the plan has not started */
    public record Consistency(String thisMonth, int currentStreak, int longestStreak, int missedMonths,
                              LocalDate dueDate) {
    }

    public record Buy(LocalDate date, long amountCents, BigDecimal shares, BigDecimal price) {
    }

    /** @param ref stable reference such as "journal#12", so an answer can point back at the source */
    public record Entry(String ref, LocalDate date, String type, Short mood, List<String> tags, String text) {
    }

    public record Note(String ref, LocalDate date, String title, String source, List<String> tags, String text) {
    }

    public record Cards(long total, long due, long newCards) {
    }
}
