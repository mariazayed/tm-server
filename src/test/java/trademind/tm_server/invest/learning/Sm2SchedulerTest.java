package trademind.tm_server.invest.learning;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.learning.Sm2Scheduler.Grade;
import trademind.tm_server.invest.learning.Sm2Scheduler.Result;
import trademind.tm_server.invest.learning.Sm2Scheduler.State;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sm2SchedulerTest {

    private static final LocalDate DAY = LocalDate.parse("2026-10-01");

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void goodReviewsFollowTheOneSixThenEaseTimesIntervalLadder() {
        Result r1 = Sm2Scheduler.review(State.fresh(), Grade.GOOD, DAY);
        assertEquals(new State(1, 1, d("2.50"), 0), r1.state());
        assertEquals(LocalDate.parse("2026-10-02"), r1.dueOn());

        Result r2 = Sm2Scheduler.review(r1.state(), Grade.GOOD, DAY);
        assertEquals(6, r2.state().intervalDays());
        assertEquals(LocalDate.parse("2026-10-07"), r2.dueOn());

        Result r3 = Sm2Scheduler.review(r2.state(), Grade.GOOD, DAY);
        assertEquals(15, r3.state().intervalDays()); // 6 * 2.5
        assertEquals(3, r3.state().repetitions());
        assertEquals(d("2.50"), r3.state().ease()); // Good leaves ease alone
    }

    @Test
    void easyRaisesEaseAndUsesTheOldEaseForTheInterval() {
        Result r = Sm2Scheduler.review(new State(3, 15, d("2.50"), 0), Grade.EASY, DAY);
        assertEquals(38, r.state().intervalDays()); // 15 * 2.50 = 37.5, rounded half up, old ease
        assertEquals(d("2.60"), r.state().ease());
        assertEquals(LocalDate.parse("2026-11-08"), r.dueOn());
    }

    @Test
    void hardLowersEaseButStillPasses() {
        Result r = Sm2Scheduler.review(new State(3, 15, d("2.50"), 0), Grade.HARD, DAY);
        assertEquals(4, r.state().repetitions());
        assertEquals(38, r.state().intervalDays());
        assertEquals(d("2.36"), r.state().ease());
    }

    @Test
    void againResetsTheCardTomorrowCountsALapseAndKeepsEase() {
        Result r = Sm2Scheduler.review(new State(3, 15, d("2.50"), 0), Grade.AGAIN, DAY);
        assertEquals(new State(0, 1, d("2.50"), 1), r.state());
        assertEquals(LocalDate.parse("2026-10-02"), r.dueOn());
    }

    @Test
    void failingABrandNewCardIsNotALapse() {
        Result r = Sm2Scheduler.review(State.fresh(), Grade.AGAIN, DAY);
        assertEquals(0, r.state().lapses());
        assertEquals(1, r.state().intervalDays());
    }

    @Test
    void firstReviewOfEachGrade() {
        assertEquals(d("2.60"), Sm2Scheduler.review(State.fresh(), Grade.EASY, DAY).state().ease());
        assertEquals(d("2.36"), Sm2Scheduler.review(State.fresh(), Grade.HARD, DAY).state().ease());
        assertEquals(1, Sm2Scheduler.review(State.fresh(), Grade.HARD, DAY).state().intervalDays());
    }

    @Test
    void easeNeverDropsBelowTheFloor() {
        State s = new State(5, 10, d("1.30"), 0);
        assertEquals(d("1.30"), Sm2Scheduler.review(s, Grade.HARD, DAY).state().ease());
        State almost = new State(5, 10, d("1.40"), 0);
        assertEquals(d("1.30"), Sm2Scheduler.review(almost, Grade.HARD, DAY).state().ease()); // 1.40 - 0.14 -> 1.26 -> 1.30
    }

    @Test
    void intervalAlwaysMovesForwardEvenWithLowEase() {
        // 1.3 * 1 rounds to 1, which would never grow.
        assertEquals(2, Sm2Scheduler.review(new State(2, 1, d("1.30"), 0), Grade.GOOD, DAY).state().intervalDays());
    }

    @Test
    void intervalIsCapped() {
        assertEquals(Sm2Scheduler.MAX_INTERVAL_DAYS,
                Sm2Scheduler.review(new State(9, 3000, d("2.50"), 0), Grade.GOOD, DAY).state().intervalDays());
    }

    @Test
    void recoveringAfterALapseClimbsTheLadderAgain() {
        State s = Sm2Scheduler.review(new State(4, 40, d("2.50"), 0), Grade.AGAIN, DAY).state();
        s = Sm2Scheduler.review(s, Grade.GOOD, DAY).state();
        assertEquals(1, s.intervalDays());
        s = Sm2Scheduler.review(s, Grade.GOOD, DAY).state();
        assertEquals(6, s.intervalDays());
        assertEquals(1, s.lapses());
    }
}
