package trademind.tm_server.invest.reminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReminderPlannerTest {

    private static final Set<ReminderKind> NONE = EnumSet.noneOf(ReminderKind.class);
    private static final Set<ReminderKind> DUE = EnumSet.of(ReminderKind.DUE);
    private static final Set<ReminderKind> BOTH = EnumSet.of(ReminderKind.DUE, ReminderKind.FOLLOWUP);

    private static Optional<ReminderKind> plan(String today, int buyDay, boolean done, Set<ReminderKind> sent) {
        return ReminderPlanner.next(LocalDate.parse(today), buyDay, 3, done, sent);
    }

    @Test
    void nothingBeforeTheBuyDay() {
        assertEquals(Optional.empty(), plan("2026-10-14", 15, false, NONE));
    }

    @Test
    void dueOnTheBuyDay() {
        assertEquals(Optional.of(ReminderKind.DUE), plan("2026-10-15", 15, false, NONE));
    }

    @Test
    void dueIsNotRepeatedDuringTheGapBeforeTheFollowup() {
        assertEquals(Optional.empty(), plan("2026-10-15", 15, false, DUE));
        assertEquals(Optional.empty(), plan("2026-10-17", 15, false, DUE));
    }

    @Test
    void followupThreeDaysLater() {
        assertEquals(Optional.of(ReminderKind.FOLLOWUP), plan("2026-10-18", 15, false, DUE));
    }

    @Test
    void followupIsSentOnceAndThenTheMonthIsQuiet() {
        assertEquals(Optional.empty(), plan("2026-10-18", 15, false, BOTH));
        assertEquals(Optional.empty(), plan("2026-10-31", 15, false, BOTH));
    }

    @Test
    void contributingSilencesEverything() {
        assertEquals(Optional.empty(), plan("2026-10-15", 15, true, NONE));
        assertEquals(Optional.empty(), plan("2026-10-18", 15, true, DUE));
    }

    @Test
    void aMissedRunSendsOnlyTheMostRelevantReminder() {
        // The server was down on the buy day and its follow-up day: one nudge, not two.
        assertEquals(Optional.of(ReminderKind.FOLLOWUP), plan("2026-10-20", 15, false, NONE));
        // Down only on the buy day: DUE goes out late.
        assertEquals(Optional.of(ReminderKind.DUE), plan("2026-10-16", 15, false, NONE));
    }

    @Test
    void followupNeverSpillsIntoTheNextMonth() {
        // Buy day 28 + 3 would be Nov 1; it is capped at the end of October.
        assertEquals(Optional.of(ReminderKind.DUE), plan("2026-10-28", 28, false, NONE));
        assertEquals(Optional.of(ReminderKind.FOLLOWUP), plan("2026-10-31", 28, false, DUE));
    }

    @Test
    void shortMonthsCollapseTheFollowupInsteadOfDoubleSending() {
        // February 2027 has 28 days: due on the 28th, which is also the last day, so no follow-up.
        assertEquals(Optional.of(ReminderKind.DUE), plan("2027-02-28", 28, false, NONE));
        assertEquals(Optional.empty(), plan("2027-02-28", 28, false, DUE));
    }

    @Test
    void aNewMonthStartsFresh() {
        assertEquals(Optional.empty(), plan("2026-11-02", 15, false, NONE));
        assertEquals(Optional.of(ReminderKind.DUE), plan("2026-11-15", 15, false, NONE));
    }
}
