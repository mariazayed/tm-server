package trademind.tm_server.invest.journal;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator.MonthState;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckinTemplateBuilderTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    @Test
    void doneMonthShowsTheBuyAndStreak() {
        String body = CheckinTemplateBuilder.build(OCT, MonthState.DONE, 1, 30000, "USD", 3);
        assertTrue(body.startsWith("## Month check-in: October 2026"));
        assertTrue(body.contains("**Contribution:** done (1 buy, $300.00)"));
        assertTrue(body.contains("**Streak:** 3 months"));
    }

    @Test
    void containsTheFourPrompts() {
        String body = CheckinTemplateBuilder.build(OCT, MonthState.DONE, 1, 30000, "USD", 1);
        for (String prompt : new String[] {"### Did I stick to the plan?", "### How do I feel about the market?",
                "### What did I learn this month?", "### Any changes to make? (default: none)"}) {
            assertTrue(body.contains(prompt), prompt);
        }
        assertTrue(body.contains("**Streak:** 1 month\n"));
    }

    @Test
    void describesEveryState() {
        assertEquals("done (2 buys, $600.00)", CheckinTemplateBuilder.describe(MonthState.DONE, 2, 60000, "USD"));
        assertEquals("not yet bought this month", CheckinTemplateBuilder.describe(MonthState.PENDING, 0, 0, "USD"));
        assertEquals("overdue, nothing bought yet this month",
                CheckinTemplateBuilder.describe(MonthState.OVERDUE, 0, 0, "USD"));
        assertEquals("missed", CheckinTemplateBuilder.describe(MonthState.MISSED, 0, 0, "USD"));
    }

    @Test
    void unknownCurrencyCodeDoesNotCrash() {
        assertTrue(CheckinTemplateBuilder.money(12345, "ZZZ").contains("123.45"));
    }
}
