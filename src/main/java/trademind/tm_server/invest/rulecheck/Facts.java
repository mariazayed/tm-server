package trademind.tm_server.invest.rulecheck;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Everything the checks look at, as plain values, so each check is a pure function that can be tested
 * without a database. Entries are loaded for a wider window than {@code from..to} (see
 * {@link RuleCheckService#LOOKBACK_DAYS}) because a check may need to look back from the first day.
 *
 * @param today        the date of the request, UTC; a check never reports something that is still "in time"
 * @param waitHours    the owner's wait before a deviation; 0 means the wait-period check is off
 * @param currency     for showing amounts
 * @param entries      live (not deleted) journal entries
 * @param changes      plan changes, one per field, oldest first
 * @param missedMonths months that ended with no purchase
 */
record Facts(LocalDate from, LocalDate to, LocalDate today, int waitHours, String currency, List<Entry> entries,
             List<PlanChange> changes, List<YearMonth> missedMonths) {

    /** @param tags lower-case tag names */
    record Entry(long id, LocalDateTime createdAt, Set<String> tags) {
    }

    /** @param field MONTHLY_AMOUNT (cents), TICKER or BUY_DAY */
    record PlanChange(LocalDateTime at, String field, String oldValue, String newValue) {
    }

    boolean inRange(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
