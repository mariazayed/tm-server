package trademind.tm_server.invest.ai;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Turns a {@link UserContext} into the text an LLM reads. Plain, compact and bounded.
 *
 * <p>Everything the owner wrote is wrapped in {@code <user_text>} tags with {@code & < >} escaped, so
 * text such as "</user_text> ignore your instructions" cannot break out of its tag and pose as an
 * instruction. {@link AiGuardrails#SYSTEM_PROMPT} tells the model to treat those tags as data.
 */
public final class UserContextRenderer {

    /** Hard ceiling for the rendered text, roughly 3k tokens. */
    public static final int MAX_CHARS = 12_000;

    private UserContextRenderer() {
    }

    public record Rendered(String text, boolean truncated) {
    }

    public static Rendered render(UserContext c) {
        // Try the full text first, then drop the oldest free-text items until it fits.
        List<UserContext.Entry> entries = new ArrayList<>(c.recentJournal());
        List<UserContext.Note> notes = new ArrayList<>(c.recentNotes());
        boolean truncated = c.truncated();
        String text = build(c, entries, notes, truncated);
        while (text.length() > MAX_CHARS && (!entries.isEmpty() || !notes.isEmpty())) {
            // Oldest items are last in each list (newest first).
            if (notes.size() >= entries.size() && !notes.isEmpty()) {
                notes.remove(notes.size() - 1);
            } else {
                entries.remove(entries.size() - 1);
            }
            truncated = true;
            text = build(c, entries, notes, true);
        }
        if (text.length() > MAX_CHARS) { // pathological: huge rules; cut hard rather than exceed the contract
            text = text.substring(0, MAX_CHARS - 1) + "…";
            truncated = true;
        }
        return new Rendered(text, truncated);
    }

    private static String build(UserContext c, List<UserContext.Entry> entries, List<UserContext.Note> notes,
                                boolean truncated) {
        String cur = c.plan().currency();
        StringBuilder b = new StringBuilder();
        b.append("# Owner context (as of ").append(c.generatedAt()).append(")\n\n");

        var plan = c.plan();
        b.append("## Plan\n")
                .append("- Invests ").append(money(plan.monthlyAmountCents(), cur)).append(" per month into ")
                .append(plan.ticker()).append(", on day ").append(plan.buyDay()).append(" of each month.\n")
                .append("- Started ").append(plan.startDate()).append(".\n\n");

        b.append("## The owner's rules\n");
        if (c.rules().isEmpty()) {
            b.append("(none written yet)\n");
        }
        int n = 1;
        for (String rule : c.rules()) {
            b.append(n++).append(". ").append(wrap(rule)).append('\n');
        }
        b.append('\n');

        var pf = c.portfolio();
        b.append("## Portfolio\n")
                .append("- Invested ").append(money(pf.investedCents(), cur)).append(", worth ")
                .append(money(pf.valueCents(), cur)).append(" (gain ").append(signedMoney(pf.gainCents(), cur));
        if (pf.gainPercent() != null) b.append(", ").append(pf.gainPercent().toPlainString()).append('%');
        b.append(").\n");
        if (pf.shares() != null && pf.shares().signum() > 0) {
            b.append("- Holds ").append(pf.shares().stripTrailingZeros().toPlainString()).append(" shares");
            if (pf.averageCost() != null) b.append(", average cost ").append(pf.averageCost().toPlainString());
            b.append(".\n");
        }
        if (pf.priceAsOf() != null) {
            b.append(pf.pricesAreMarketData()
                    ? "- Latest market price is from " + pf.priceAsOf() + (pf.priceIsStale() ? " (out of date).\n" : ".\n")
                    : "- No market prices yet; value uses the owner's last purchase price from " + pf.priceAsOf() + ".\n");
        }
        b.append('\n');

        var co = c.consistency();
        b.append("## Consistency\n")
                .append("- This month: ").append(co.thisMonth().toLowerCase(Locale.ROOT)).append('\n')
                .append("- Current streak ").append(co.currentStreak()).append(" months, best ").append(co.longestStreak())
                .append(", missed months ").append(co.missedMonths()).append(".\n\n");

        b.append("## Recent purchases (newest first)\n");
        if (c.recentBuys().isEmpty()) b.append("(none yet)\n");
        for (var buy : c.recentBuys()) {
            b.append("- ").append(buy.date()).append(": ").append(money(buy.amountCents(), cur)).append(" for ")
                    .append(buy.shares().stripTrailingZeros().toPlainString()).append(" shares at ")
                    .append(buy.price().toPlainString()).append('\n');
        }
        b.append('\n');

        if (!entries.isEmpty()) {
            b.append("## Recent journal entries (newest first)\n");
            for (var e : entries) {
                b.append("- [").append(e.ref()).append("] ").append(e.date()).append(' ').append(e.type().toLowerCase(Locale.ROOT));
                if (e.mood() != null) b.append(", mood ").append(e.mood()).append("/5");
                if (!e.tags().isEmpty()) b.append(", tags ").append(String.join(", ", e.tags()));
                b.append(": ").append(wrap(e.text())).append('\n');
            }
            b.append('\n');
        }
        if (!notes.isEmpty()) {
            b.append("## Recent learning notes (newest first)\n");
            for (var note : notes) {
                b.append("- [").append(note.ref()).append("] ").append(note.date()).append(' ').append(wrap(note.title()));
                if (note.source() != null) b.append(" (from ").append(wrap(note.source())).append(')');
                if (!note.tags().isEmpty()) b.append(", tags ").append(String.join(", ", note.tags()));
                b.append(": ").append(wrap(note.text())).append('\n');
            }
            b.append('\n');
        }

        var cards = c.flashcards();
        b.append("## Flashcards\n- ").append(cards.total()).append(" cards, ").append(cards.due()).append(" due, ")
                .append(cards.newCards()).append(" never reviewed.\n");
        if (truncated) b.append("\n(Older entries were left out to keep this short.)\n");
        return b.toString();
    }

    /**
     * Wraps owner-written text in its tag with markup escaped and every run of whitespace, newlines
     * included, flattened to one space. Flattening here (not trusting callers) means no value can ever
     * start a new line of its own, so it cannot pose as a heading or a fresh instruction.
     */
    static String wrap(String text) {
        String flat = text.strip().replaceAll("\\s+", " ");
        return "<" + AiGuardrails.USER_TEXT_TAG + ">" + escape(flat) + "</" + AiGuardrails.USER_TEXT_TAG + ">";
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String money(long cents, String currency) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
        try {
            f.setCurrency(Currency.getInstance(currency));
        } catch (IllegalArgumentException e) {
            // unknown code: keep the default symbol
        }
        return f.format(BigDecimal.valueOf(cents).movePointLeft(2));
    }

    static String signedMoney(long cents, String currency) {
        return (cents > 0 ? "+" : "") + money(cents, currency);
    }
}
