package trademind.tm_server.invest.contribution;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Month-by-month consistency of the contribution plan. Pure logic, no I/O.
 *
 * <p>A calendar month is DONE once it holds at least one contribution. A past month without one is
 * MISSED. The current month is DONE, PENDING (buy day not reached yet) or OVERDUE (buy day passed);
 * pending and overdue never break the streak, so you keep it until the month actually ends.
 */
public final class ContributionStatusCalculator {

    public enum MonthState { DONE, MISSED, PENDING, OVERDUE }

    public record Entry(LocalDate date, long amountCents) {
    }

    public record Month(YearMonth month, MonthState state, int contributions, long totalCents) {
    }

    /**
     * @param daysUntilDue days from today to this month's buy day; negative once it has passed;
     *                     null when there are no months yet (plan starts in the future)
     */
    public record Result(List<Month> months, int currentStreak, int longestStreak, int missedCount,
                         LocalDate currentDueDate, Long daysUntilDue) {
    }

    private ContributionStatusCalculator() {
    }

    public static Result calculate(LocalDate startDate, int buyDay, LocalDate today,
                                   Collection<Entry> entries) {
        YearMonth current = YearMonth.from(today);
        Map<YearMonth, long[]> byMonth = new TreeMap<>(); // [count, totalCents]
        for (Entry e : entries) {
            long[] agg = byMonth.computeIfAbsent(YearMonth.from(e.date()), k -> new long[2]);
            agg[0]++;
            agg[1] += e.amountCents();
        }

        YearMonth first = YearMonth.from(startDate);
        if (!byMonth.isEmpty() && byMonth.keySet().iterator().next().isBefore(first)) {
            first = byMonth.keySet().iterator().next();
        }
        if (first.isAfter(current)) {
            return new Result(List.of(), 0, 0, 0, null, null);
        }

        List<Month> months = new ArrayList<>();
        LocalDate dueDate = dueDate(current, buyDay);
        int missed = 0;
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            long[] agg = byMonth.getOrDefault(m, new long[2]);
            MonthState state;
            if (agg[0] > 0) {
                state = MonthState.DONE;
            } else if (m.isBefore(current)) {
                state = MonthState.MISSED;
                missed++;
            } else {
                state = today.isAfter(dueDate) ? MonthState.OVERDUE : MonthState.PENDING;
            }
            months.add(new Month(m, state, (int) agg[0], agg[1]));
        }

        int longest = 0;
        int run = 0;
        for (Month m : months) {
            run = m.state() == MonthState.DONE ? run + 1 : 0;
            longest = Math.max(longest, run);
        }

        int streak = 0;
        for (int i = months.size() - 1; i >= 0; i--) {
            MonthState s = months.get(i).state();
            if (i == months.size() - 1 && (s == MonthState.PENDING || s == MonthState.OVERDUE)) {
                continue; // current month still open: neither counts nor breaks the streak
            }
            if (s != MonthState.DONE) break;
            streak++;
        }

        return new Result(months, streak, longest, missed, dueDate,
                ChronoUnit.DAYS.between(today, dueDate));
    }

    static LocalDate dueDate(YearMonth month, int buyDay) {
        return month.atDay(Math.min(buyDay, month.lengthOfMonth()));
    }
}
