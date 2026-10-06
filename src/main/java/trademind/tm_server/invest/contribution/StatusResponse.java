package trademind.tm_server.invest.contribution;

import java.time.LocalDate;
import java.util.List;

/** JSON shape of the consistency status; months are "YYYY-MM". */
public record StatusResponse(
        List<MonthStatus> months,
        int currentStreak,
        int longestStreak,
        int missedCount,
        LocalDate currentDueDate,
        Long daysUntilDue) {

    public record MonthStatus(String month, String state, int contributions, long totalCents) {
    }

    static StatusResponse from(ContributionStatusCalculator.Result r) {
        return new StatusResponse(
                r.months().stream()
                        .map(m -> new MonthStatus(m.month().toString(), m.state().name(),
                                m.contributions(), m.totalCents()))
                        .toList(),
                r.currentStreak(), r.longestStreak(), r.missedCount(), r.currentDueDate(), r.daysUntilDue());
    }
}
