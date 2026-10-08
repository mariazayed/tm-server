package trademind.tm_server.invest.checkin;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckinFactsRendererTest {

    private static CheckinFacts.Buy buy(String date, long cents, String shares, String price, long fee) {
        return new CheckinFacts.Buy(LocalDate.parse(date), cents, new BigDecimal(shares), new BigDecimal(price), fee);
    }

    private static CheckinFacts facts(List<CheckinFacts.Buy> buys, Integer daysAfterDue, CheckinFacts.MonthEndValue value,
                                      int entries, List<Integer> moods, String moodAvg, List<CheckinFacts.TagCount> tags, List<String> notes) {
        long invested = buys.stream().mapToLong(CheckinFacts.Buy::amountCents).sum();
        return new CheckinFacts("2026-09", "USD", 30000, 1, LocalDate.parse("2026-09-01"), buys.isEmpty() ? "MISSED" : "DONE",
                buys, invested, buys.stream().mapToLong(CheckinFacts.Buy::feeCents).sum(),
                buys.stream().map(CheckinFacts.Buy::shares).reduce(BigDecimal.ZERO, BigDecimal::add),
                buys.isEmpty() ? null : new BigDecimal("117.4250"), buys.isEmpty() ? null : buys.get(0).date(), daysAfterDue,
                3, 5, 1, value, entries, moods, moodAvg == null ? null : new BigDecimal(moodAvg), tags, notes, buys.isEmpty() ? null : 9L);
    }

    @Test
    void aFullMonthIsDescribedLineByLine() {
        CheckinFacts f = facts(List.of(buy("2026-09-05", 30000, "2.55489022", "117.4250", 0)), 4,
                new CheckinFacts.MonthEndValue(LocalDate.parse("2026-09-30"), 90000, 96215, 6215, new BigDecimal("6.91")),
                4, List.of(2, 3, 4, 4), "3.3", List.of(new CheckinFacts.TagCount("fear", 2), new CheckinFacts.TagCount("decision", 1)),
                List.of("The plan was changed on 2026-09-12 (ticker VT to VTI), with no journal entry."));
        assertEquals(List.of(
                "Plan: 300.00 USD per month, buy day 1 (due 2026-09-01).",
                "Status: DONE.",
                "Bought: 2026-09-05, 300.00 USD for 2.55489022 shares at 117.4250 (fee 0.00 USD).",
                "Invested this month: 300.00 USD (plan 300.00 USD), fees 0.00 USD, average price 117.4250.",
                "Timing: first buy 4 days after the buy day.",
                "Streak: current 3 months, longest 5, missed months so far 1.",
                "Value at 2026-09-30: invested to date 900.00 USD, worth 962.15 USD, gain +62.15 USD (+6.91%).",
                "Journal: 4 entries; moods 2, 3, 4, 4 (average 3.3).",
                "Tags: fear 2, decision 1.",
                "Rule note: The plan was changed on 2026-09-12 (ticker VT to VTI), with no journal entry."),
                CheckinFactsRenderer.lines(f));
    }

    @Test
    void aMonthWithNothingSaysSoForEachPart() {
        List<String> lines = CheckinFactsRenderer.lines(facts(List.of(), null, null, 0, List.of(), null, List.of(), List.of()));
        assertTrue(lines.contains("Bought: nothing this month."));
        assertTrue(lines.contains("Journal: no entries this month."));
        assertTrue(lines.contains("Rule notes: none."));
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Timing:") || l.startsWith("Value at") || l.startsWith("Tags:")));
    }

    @Test
    void timingReadsEarlyOnTimeAndLate() {
        assertEquals("was on the buy day", CheckinFactsRenderer.timing(0));
        assertEquals("1 day after the buy day", CheckinFactsRenderer.timing(1));
        assertEquals("12 days after the buy day", CheckinFactsRenderer.timing(12));
        assertEquals("2 days before the buy day", CheckinFactsRenderer.timing(-2));
        assertEquals("1 day before the buy day", CheckinFactsRenderer.timing(-1));
    }

    @Test
    void aLossIsShownWithItsOwnSignAndNoPlus() {
        CheckinFacts f = facts(List.of(buy("2026-09-01", 30000, "2", "150.0000", 100)), 0,
                new CheckinFacts.MonthEndValue(LocalDate.parse("2026-09-30"), 30100, 27000, -3100, new BigDecimal("-10.30")),
                1, List.of(), null, List.of(), List.of());
        String value = CheckinFactsRenderer.lines(f).stream().filter(l -> l.startsWith("Value at")).findFirst().orElseThrow();
        assertEquals("Value at 2026-09-30: invested to date 301.00 USD, worth 270.00 USD, gain -31.00 USD (-10.30%).", value);
    }

    @Test
    void oneEntryAndNoMoodsAreWordedCorrectly() {
        List<String> lines = CheckinFactsRenderer.lines(facts(List.of(buy("2026-09-01", 30000, "2", "150.0000", 0)), 0, null,
                1, List.of(), null, List.of(), List.of()));
        assertTrue(lines.contains("Journal: 1 entry; no moods recorded."), lines.toString());
    }

    @Test
    void moneyHasTwoDecimalsAndNoSeparatorsOrSymbol() {
        assertEquals("1234567.80 USD", CheckinFactsRenderer.money(123456780, "USD"));
        assertEquals("0.05 EUR", CheckinFactsRenderer.money(5, "EUR"));
        assertEquals("+0.00 USD".replace("+", ""), CheckinFactsRenderer.money(0, "USD"));
    }
}
