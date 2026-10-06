package trademind.tm_server.invest.reminder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which single reminder, if any, should go out today. Pure logic, no I/O.
 *
 * <ul>
 *   <li>Nothing is sent once the month has a contribution.</li>
 *   <li>DUE goes out on the buy day (or the first run after it).</li>
 *   <li>FOLLOWUP goes out {@code followupDays} later, but never past the end of the month, and
 *       never when that would be the buy day itself.</li>
 *   <li>If a run was missed, only the most relevant reminder is sent, not both at once.</li>
 *   <li>Each kind is sent at most once per month.</li>
 * </ul>
 */
public final class ReminderPlanner {

    private ReminderPlanner() {
    }

    public static Optional<ReminderKind> next(LocalDate today, int buyDay, int followupDays,
                                              boolean monthDone, Set<ReminderKind> alreadySent) {
        if (monthDone) return Optional.empty();
        YearMonth month = YearMonth.from(today);
        LocalDate due = month.atDay(Math.min(buyDay, month.lengthOfMonth()));
        LocalDate followup = due.plusDays(followupDays);
        if (followup.isAfter(month.atEndOfMonth())) followup = month.atEndOfMonth();
        boolean hasFollowup = followup.isAfter(due);

        if (today.isBefore(due)) return Optional.empty();
        if (hasFollowup && !today.isBefore(followup)) {
            return alreadySent.contains(ReminderKind.FOLLOWUP) ? Optional.empty() : Optional.of(ReminderKind.FOLLOWUP);
        }
        return alreadySent.contains(ReminderKind.DUE) ? Optional.empty() : Optional.of(ReminderKind.DUE);
    }
}
