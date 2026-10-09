package trademind.tm_server.invest.rulecheck;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * A month that ended with no purchase should have a journal entry saying why. A missed month is a
 * finding if no entry of any kind was written during that month or in the {@value #GRACE_DAYS} days
 * after it. It is only reported once those days have passed, so the owner is not told off while there is
 * still time to write the note.
 */
final class MissedMonthCheck implements RuleCheck {

    static final String CODE = "MISSED_MONTH";
    static final int GRACE_DAYS = 7;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "Missed month without a note";
    }

    @Override
    public List<RuleFinding> run(Facts facts) {
        List<RuleFinding> out = new ArrayList<>();
        for (YearMonth month : facts.missedMonths()) {
            LocalDate end = month.atEndOfMonth();
            LocalDate graceEnds = end.plusDays(GRACE_DAYS);
            if (!facts.today().isAfter(graceEnds) || !facts.inRange(end)) continue;
            LocalDateTime from = month.atDay(1).atStartOfDay();
            LocalDateTime until = graceEnds.plusDays(1).atStartOfDay();
            boolean noted = facts.entries().stream()
                    .anyMatch(e -> !e.createdAt().isBefore(from) && e.createdAt().isBefore(until));
            if (noted) continue;
            out.add(RuleFinding.deterministic(CODE, end,
                    "No purchase was recorded for " + month + ", and there is no journal entry from that month or the "
                            + "week after it saying why.",
                    List.of()));
        }
        return out;
    }
}
