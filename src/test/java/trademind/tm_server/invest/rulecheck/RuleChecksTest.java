package trademind.tm_server.invest.rulecheck;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The three deterministic checks as pure functions of dates and settings. */
class RuleChecksTest {

    private static LocalDateTime at(String text) {
        return LocalDateTime.parse(text);
    }

    private static Facts.Entry entry(long id, String when, String... tags) {
        return new Facts.Entry(id, at(when), Set.of(tags));
    }

    private static Facts facts(String from, String to, String today, int wait, List<Facts.Entry> entries,
                               List<Facts.PlanChange> changes, List<YearMonth> missed) {
        return new Facts(LocalDate.parse(from), LocalDate.parse(to), LocalDate.parse(today), wait, "USD", entries, changes, missed);
    }

    private static Facts waitFacts(int hours, Facts.Entry... entries) {
        return facts("2026-03-01", "2026-03-31", "2026-04-02", hours, List.of(entries), List.of(), List.of());
    }

    // ---- wait period ----------------------------------------------------------------------------

    private final WaitPeriodCheck waitCheck = new WaitPeriodCheck();

    @Test
    void aDeviationAfterTheFullWaitIsFine() {
        Facts f = waitFacts(72, entry(1, "2026-03-01T09:00", "decision"), entry(2, "2026-03-05T09:00", "deviation")); // 96h
        assertEquals(List.of(), waitCheck.run(f));
    }

    @Test
    void exactlyTheWaitIsFineAndOneHourLessIsNot() {
        assertEquals(List.of(), waitCheck.run(waitFacts(72, entry(1, "2026-03-01T09:00", "decision"), entry(2, "2026-03-04T09:00", "deviation"))));
        List<RuleFinding> short71 = waitCheck.run(waitFacts(72, entry(1, "2026-03-01T09:00", "decision"), entry(2, "2026-03-04T08:00", "deviation")));
        assertEquals(1, short71.size());
        RuleFinding finding = short71.get(0);
        assertEquals("WAIT_PERIOD", finding.check());
        assertEquals("DETERMINISTIC", finding.source());
        assertEquals("HIGH", finding.confidence());
        assertEquals(LocalDate.parse("2026-03-04"), finding.date());
        assertEquals(List.of(1L, 2L), finding.entryIds());
        assertNull(finding.ruleId());
        assertNull(finding.quote());
        assertEquals("Entry 2 (2026-03-04) records a deviation only 71 hours after the decision in entry 1 (2026-03-01). "
                + "Your wait is 72 hours.", finding.message());
    }

    @Test
    void aDeviationRightAfterTheDecisionSaysSo() {
        List<RuleFinding> same = waitCheck.run(waitFacts(72, entry(1, "2026-03-04T09:00", "decision"), entry(2, "2026-03-04T09:30", "deviation")));
        assertTrue(same.get(0).message().contains("less than an hour after the decision"), same.get(0).message());
        List<RuleFinding> one = waitCheck.run(waitFacts(72, entry(1, "2026-03-04T09:00", "decision"), entry(2, "2026-03-04T10:00", "deviation")));
        assertTrue(one.get(0).message().contains("only 1 hour after"), one.get(0).message());
    }

    @Test
    void noDecisionEntryIsAFinding() {
        List<RuleFinding> out = waitCheck.run(waitFacts(72, entry(2, "2026-03-04T09:00", "deviation")));
        assertEquals(1, out.size());
        assertEquals(List.of(2L), out.get(0).entryIds());
        assertTrue(out.get(0).message().contains("no decision entry in the 14 days before it"), out.get(0).message());
    }

    @Test
    void aDecisionOlderThanFourteenDaysIsTreatedAsUnrelated() {
        Facts old = waitFacts(72, entry(1, "2026-02-18T09:00", "decision"), entry(2, "2026-03-04T09:00", "deviation")); // 14d 0h
        assertEquals(List.of(), waitCheck.run(old), "exactly 14 days back still counts");
        Facts older = waitFacts(72, entry(1, "2026-02-18T08:00", "decision"), entry(2, "2026-03-04T09:00", "deviation"));
        assertEquals(1, waitCheck.run(older).size());
        assertTrue(waitCheck.run(older).get(0).message().contains("no decision entry"));
    }

    @Test
    void theEarliestRecentDecisionStartsTheClock() {
        // Decided on the 1st, had second thoughts on the 3rd, acted on the 5th: 96 hours since the first decision.
        Facts f = waitFacts(72, entry(1, "2026-03-01T09:00", "decision"), entry(3, "2026-03-03T09:00", "decision"),
                entry(2, "2026-03-05T09:00", "deviation"));
        assertEquals(List.of(), waitCheck.run(f));
    }

    @Test
    void aDecisionWrittenAfterTheDeviationDoesNotCount() {
        List<RuleFinding> out = waitCheck.run(waitFacts(72, entry(2, "2026-03-04T09:00", "deviation"), entry(1, "2026-03-04T10:00", "decision")));
        assertTrue(out.get(0).message().contains("no decision entry"));
    }

    @Test
    void anEntryTaggedBothIsADeviationNotADecision() {
        List<RuleFinding> out = waitCheck.run(waitFacts(72, entry(1, "2026-03-01T09:00", "decision", "deviation"),
                entry(2, "2026-03-05T09:00", "deviation")));
        assertEquals(2, out.size(), "neither has a plain decision before it");
    }

    @Test
    void onlyDeviationsInTheRangeAreReportedButEarlierEntriesStillCountAsDecisions() {
        List<Facts.Entry> entries = List.of(entry(1, "2026-03-01T09:00", "decision"),
                entry(2, "2026-03-02T09:00", "deviation"),   // 24h after the decision: too soon
                entry(3, "2026-03-12T09:00", "deviation"));  // 11 days after it: fine
        Facts narrow = facts("2026-03-10", "2026-03-31", "2026-04-02", 72, entries, List.of(), List.of());
        assertEquals(List.of(), waitCheck.run(narrow), "entry 2 is before the range, and entry 3 used entry 1 from before it");
        Facts wide = facts("2026-03-01", "2026-03-31", "2026-04-02", 72, entries, List.of(), List.of());
        assertEquals(List.of(List.of(1L, 2L)), waitCheck.run(wide).stream().map(RuleFinding::entryIds).toList());
    }

    @Test
    void aWaitOfZeroSwitchesTheCheckOff() {
        Facts f = waitFacts(0, entry(2, "2026-03-04T09:00", "deviation"));
        assertEquals(List.of(), waitCheck.run(f));
        assertEquals("Switched off: the wait is set to 0 hours in Settings.", waitCheck.offReason(f));
        assertNull(waitCheck.offReason(waitFacts(72)));
    }

    // ---- plan changes ---------------------------------------------------------------------------

    private final PlanChangeCheck planCheck = new PlanChangeCheck();

    private static Facts plan(List<Facts.Entry> entries, Facts.PlanChange... changes) {
        return facts("2026-03-01", "2026-03-31", "2026-04-02", 72, entries, List.of(changes), List.of());
    }

    private static Facts.PlanChange amount(String when, String from, String to) {
        return new Facts.PlanChange(at(when), "MONTHLY_AMOUNT", from, to);
    }

    @Test
    void aChangeWithAnEntryInTheWeekBeforeIsFine() {
        Facts f = plan(List.of(entry(1, "2026-03-07T09:00")), amount("2026-03-10T12:00", "30000", "40000"));
        assertEquals(List.of(), planCheck.run(f));
    }

    @Test
    void theWindowIsSevenDaysInclusiveAndOnlyEntriesBeforeTheChangeCount() {
        Facts.PlanChange change = amount("2026-03-10T12:00", "30000", "40000");
        assertEquals(List.of(), planCheck.run(plan(List.of(entry(1, "2026-03-03T12:00")), change)), "exactly 7 days before");
        assertEquals(1, planCheck.run(plan(List.of(entry(1, "2026-03-03T11:59")), change)).size(), "a minute older");
        assertEquals(1, planCheck.run(plan(List.of(entry(1, "2026-03-10T12:01")), change)).size(), "written after the change");
        assertEquals(List.of(), planCheck.run(plan(List.of(entry(1, "2026-03-10T12:00")), change)), "same instant counts");
    }

    @Test
    void noEntryAtAllIsAFindingThatShowsOldAndNewValues() {
        List<RuleFinding> out = planCheck.run(plan(List.of(), amount("2026-03-10T12:00", "30000", "40050")));
        assertEquals(1, out.size());
        RuleFinding f = out.get(0);
        assertEquals("PLAN_CHANGE", f.check());
        assertEquals(LocalDate.parse("2026-03-10"), f.date());
        assertEquals(List.of(), f.entryIds());
        assertEquals("The plan was changed on 2026-03-10 (monthly amount 300.00 USD to 400.50 USD), with no journal entry "
                + "in the 7 days before it. Your rule says to write the reason first.", f.message());
    }

    @Test
    void fieldsSavedTogetherAreOneFindingAndSeparateSavesAreSeparate() {
        List<RuleFinding> together = planCheck.run(plan(List.of(),
                amount("2026-03-10T12:00:00", "30000", "40000"),
                new Facts.PlanChange(at("2026-03-10T12:00:01"), "TICKER", "VT", "VTI"),
                new Facts.PlanChange(at("2026-03-10T12:00:01"), "BUY_DAY", "1", "5")));
        assertEquals(1, together.size());
        String m = together.get(0).message();
        assertTrue(m.contains("monthly amount 300.00 USD to 400.00 USD; ticker VT to VTI; buy day 1 to 5"), m);

        List<RuleFinding> apart = planCheck.run(plan(List.of(),
                amount("2026-03-10T12:00:00", "30000", "40000"), amount("2026-03-10T12:01:00", "40000", "50000")));
        assertEquals(2, apart.size());
    }

    @Test
    void aChangeOutsideTheRangeIsNotReported() {
        Facts f = facts("2026-03-15", "2026-03-31", "2026-04-02", 72, List.of(), List.of(amount("2026-03-10T12:00", "1", "2")), List.of());
        assertEquals(List.of(), planCheck.run(f));
    }

    // ---- missed months --------------------------------------------------------------------------

    private final MissedMonthCheck missedCheck = new MissedMonthCheck();

    private static Facts missed(String today, List<Facts.Entry> entries, YearMonth... months) {
        return facts("2026-02-01", "2026-03-31", today, 72, entries, List.of(), List.of(months));
    }

    @Test
    void aMissedMonthWithNoNoteIsReportedOnceTheWeekAfterHasPassed() {
        YearMonth feb = YearMonth.of(2026, 2);
        assertEquals(List.of(), missedCheck.run(missed("2026-03-07", List.of(), feb)), "still within the week to write it");
        List<RuleFinding> out = missedCheck.run(missed("2026-03-08", List.of(), feb));
        assertEquals(1, out.size());
        RuleFinding f = out.get(0);
        assertEquals("MISSED_MONTH", f.check());
        assertEquals(LocalDate.parse("2026-02-28"), f.date());
        assertEquals("No purchase was recorded for 2026-02, and there is no journal entry from that month or the week after "
                + "it saying why.", f.message());
    }

    @Test
    void anEntryInTheMonthOrTheWeekAfterExplainsIt() {
        YearMonth feb = YearMonth.of(2026, 2);
        for (String when : List.of("2026-02-01T00:00", "2026-02-14T10:00", "2026-02-28T23:59", "2026-03-07T23:59")) {
            assertEquals(List.of(), missedCheck.run(missed("2026-03-20", List.of(entry(1, when)), feb)), when);
        }
        for (String when : List.of("2026-01-31T23:59", "2026-03-08T00:00")) {
            assertEquals(1, missedCheck.run(missed("2026-03-20", List.of(entry(1, when)), feb)).size(), when);
        }
    }

    @Test
    void leapYearFebruaryEndsOnThe29th() {
        Facts f = facts("2028-02-01", "2028-03-31", "2028-04-01", 72, List.of(), List.of(), List.of(YearMonth.of(2028, 2)));
        assertEquals(LocalDate.parse("2028-02-29"), missedCheck.run(f).get(0).date());
    }

    @Test
    void aMissedMonthThatEndedOutsideTheRangeIsNotReported() {
        Facts f = facts("2026-03-01", "2026-03-31", "2026-04-20", 72, List.of(), List.of(), List.of(YearMonth.of(2026, 2)));
        assertEquals(List.of(), missedCheck.run(f));
    }

    @Test
    void findingsAreIndependentCopiesNotSharedLists() {
        List<Long> ids = new ArrayList<>(List.of(1L));
        RuleFinding f = RuleFinding.deterministic("X", LocalDate.parse("2026-03-01"), "m", ids);
        ids.add(2L);
        assertEquals(List.of(1L), f.entryIds());
    }
}
