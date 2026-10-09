package trademind.tm_server.invest.checkin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns {@link CheckinFacts} into plain lines. The same lines are shown in the app and given to the model,
 * so the numbers on screen and the numbers the model may use are the same characters. Money is written
 * like {@code 300.00 USD}, with no symbol and no thousands separator.
 */
public final class CheckinFactsRenderer {

    private CheckinFactsRenderer() {
    }

    public static List<String> lines(CheckinFacts f) {
        List<String> out = new ArrayList<>();
        out.add("Plan: " + money(f.plannedCents(), f.currency()) + " per month, buy day " + f.buyDay() + " (due "
                + f.dueDate() + ").");
        out.add("Status: " + f.state() + ".");

        if (f.buys().isEmpty()) {
            out.add("Bought: nothing this month.");
        } else {
            for (CheckinFacts.Buy b : f.buys()) {
                out.add("Bought: " + b.date() + ", " + money(b.amountCents(), f.currency()) + " for "
                        + b.shares().stripTrailingZeros().toPlainString() + " shares at " + b.price().toPlainString()
                        + " (fee " + money(b.feeCents(), f.currency()) + ").");
            }
            out.add("Invested this month: " + money(f.investedCents(), f.currency()) + " (plan "
                    + money(f.plannedCents(), f.currency()) + "), fees " + money(f.feesCents(), f.currency())
                    + ", average price " + f.averagePrice().toPlainString() + ".");
            out.add("Timing: first buy " + timing(f.daysAfterDue()) + ".");
        }
        out.add("Streak: current " + f.currentStreak() + " " + plural(f.currentStreak(), "month", "months") + ", longest "
                + f.longestStreak() + ", missed months so far " + f.missedMonths() + ".");

        CheckinFacts.MonthEndValue v = f.monthEnd();
        if (v != null) {
            out.add("Value at " + v.asOf() + ": invested to date " + money(v.investedToDateCents(), f.currency())
                    + ", worth " + money(v.valueCents(), f.currency()) + ", gain " + signed(v.gainCents(), f.currency())
                    + (v.gainPercent() == null ? "" : " (" + (v.gainPercent().signum() > 0 ? "+" : "")
                    + v.gainPercent().toPlainString() + "%)") + ".");
        }

        if (f.entryCount() == 0) {
            out.add("Journal: no entries this month.");
        } else {
            String moods = f.moods().isEmpty() ? "no moods recorded"
                    : "moods " + f.moods().stream().map(String::valueOf).collect(Collectors.joining(", "))
                    + " (average " + f.moodAverage().toPlainString() + ")";
            out.add("Journal: " + f.entryCount() + " " + plural(f.entryCount(), "entry", "entries") + "; " + moods + ".");
            if (!f.tags().isEmpty()) {
                out.add("Tags: " + f.tags().stream().map(t -> t.name() + " " + t.count()).collect(Collectors.joining(", ")) + ".");
            }
        }
        if (f.ruleNotes().isEmpty()) {
            out.add("Rule notes: none.");
        } else {
            for (String note : f.ruleNotes()) out.add("Rule note: " + note);
        }
        return out;
    }

    static String timing(Integer daysAfterDue) {
        if (daysAfterDue == null) return "was not made";
        if (daysAfterDue == 0) return "was on the buy day";
        int d = Math.abs(daysAfterDue);
        return d + " " + plural(d, "day", "days") + (daysAfterDue > 0 ? " after" : " before") + " the buy day";
    }

    static String money(long cents, String currency) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2).toPlainString() + " " + currency;
    }

    static String signed(long cents, String currency) {
        return (cents > 0 ? "+" : "") + money(cents, currency);
    }

    private static String plural(long n, String one, String many) {
        return n == 1 ? one : many;
    }
}
