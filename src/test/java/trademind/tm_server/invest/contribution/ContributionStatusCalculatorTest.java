package trademind.tm_server.invest.contribution;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator.Entry;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator.MonthState;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator.Result;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributionStatusCalculatorTest {

    private static Entry buy(String date) {
        return new Entry(LocalDate.parse(date), 30000);
    }

    private static List<MonthState> states(Result r) {
        return r.months().stream().map(ContributionStatusCalculator.Month::state).toList();
    }

    @Test
    void firstMonthBeforeBuyDayIsPendingAndCountsDaysUntilDue() {
        Result r = ContributionStatusCalculator.calculate(
                LocalDate.parse("2026-10-01"), 15, LocalDate.parse("2026-10-10"), List.of());
        assertEquals(List.of(MonthState.PENDING), states(r));
        assertEquals(LocalDate.parse("2026-10-15"), r.currentDueDate());
        assertEquals(5L, r.daysUntilDue());
        assertEquals(0, r.currentStreak());
        assertEquals(0, r.missedCount());
    }

    @Test
    void consecutiveDoneMonthsBuildTheStreak() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-08-01"), 1,
                LocalDate.parse("2026-10-05"), List.of(buy("2026-08-02"), buy("2026-09-01"), buy("2026-10-01")));
        assertEquals(List.of(MonthState.DONE, MonthState.DONE, MonthState.DONE), states(r));
        assertEquals(3, r.currentStreak());
        assertEquals(3, r.longestStreak());
    }

    @Test
    void openCurrentMonthNeitherCountsNorBreaksTheStreak() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-08-01"), 20,
                LocalDate.parse("2026-10-05"), List.of(buy("2026-08-02"), buy("2026-09-01")));
        assertEquals(MonthState.PENDING, r.months().get(2).state());
        assertEquals(2, r.currentStreak());
    }

    @Test
    void overdueCurrentMonthKeepsPriorStreakAlive() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-08-01"), 3,
                LocalDate.parse("2026-10-05"), List.of(buy("2026-08-02"), buy("2026-09-01")));
        assertEquals(MonthState.OVERDUE, r.months().get(2).state());
        assertEquals(-2L, r.daysUntilDue());
        assertEquals(2, r.currentStreak());
    }

    @Test
    void missedMonthBreaksTheStreakAndIsCounted() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-07-01"), 1,
                LocalDate.parse("2026-10-05"),
                List.of(buy("2026-07-01"), buy("2026-08-01"), buy("2026-10-01")));
        assertEquals(List.of(MonthState.DONE, MonthState.DONE, MonthState.MISSED, MonthState.DONE), states(r));
        assertEquals(1, r.currentStreak());
        assertEquals(2, r.longestStreak());
        assertEquals(1, r.missedCount());
    }

    @Test
    void twoBuysInOneMonthCountOnceButSumUp() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-10-01"), 1,
                LocalDate.parse("2026-10-20"), List.of(buy("2026-10-01"), buy("2026-10-10")));
        assertEquals(1, r.months().size());
        assertEquals(2, r.months().get(0).contributions());
        assertEquals(60000, r.months().get(0).totalCents());
        assertEquals(1, r.currentStreak());
    }

    @Test
    void contributionBeforeStartDateExtendsTheRange() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2026-09-01"), 1,
                LocalDate.parse("2026-10-05"), List.of(buy("2026-08-15"), buy("2026-10-01")));
        assertEquals(3, r.months().size());
        assertEquals(MonthState.DONE, r.months().get(0).state());
        assertEquals(MonthState.MISSED, r.months().get(1).state());
    }

    @Test
    void planStartingInTheFutureHasNoMonths() {
        Result r = ContributionStatusCalculator.calculate(
                LocalDate.parse("2027-01-01"), 1, LocalDate.parse("2026-10-05"), List.of());
        assertTrue(r.months().isEmpty());
        assertNull(r.currentDueDate());
        assertNull(r.daysUntilDue());
    }

    @Test
    void buyDay28FallsInFebruary() {
        Result r = ContributionStatusCalculator.calculate(LocalDate.parse("2027-02-01"), 28,
                LocalDate.parse("2027-02-10"), List.of());
        assertEquals(LocalDate.parse("2027-02-28"), r.currentDueDate());
    }
}
