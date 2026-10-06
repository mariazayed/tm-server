package trademind.tm_server.invest.learning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SuperMemo-2 scheduling, one review at a time. Pure logic, no I/O.
 *
 * <p>The four grades map to SM-2 quality scores: Again 1, Hard 3, Good 4, Easy 5. A grade below 3
 * is a failure: the card starts over (repetitions 0, due again in 1 day) and, as in SM-2, its ease
 * is left alone. A pass lengthens the interval (1 day, then 6, then previous interval times ease)
 * and nudges ease by {@code 0.1 - (5-q)(0.08 + (5-q)0.02)}: Easy +0.10, Good 0, Hard -0.14, never
 * below 1.30. The interval is computed with the ease from before the update, as in the original.
 */
public final class Sm2Scheduler {

    public static final BigDecimal MIN_EASE = new BigDecimal("1.30");
    public static final BigDecimal START_EASE = new BigDecimal("2.50");
    public static final int MAX_INTERVAL_DAYS = 3650;

    public enum Grade {
        AGAIN(1), HARD(3), GOOD(4), EASY(5);

        final int quality;

        Grade(int quality) {
            this.quality = quality;
        }
    }

    public record State(int repetitions, int intervalDays, BigDecimal ease, int lapses) {
        public static State fresh() {
            return new State(0, 0, START_EASE, 0);
        }
    }

    public record Result(State state, LocalDate dueOn) {
    }

    private Sm2Scheduler() {
    }

    public static Result review(State s, Grade grade, LocalDate today) {
        int q = grade.quality;
        State next;
        if (q < 3) {
            next = new State(0, 1, s.ease(), s.repetitions() > 0 ? s.lapses() + 1 : s.lapses());
        } else {
            int repetitions = s.repetitions() + 1;
            int interval;
            if (repetitions == 1) {
                interval = 1;
            } else if (repetitions == 2) {
                interval = 6;
            } else {
                interval = s.ease().multiply(BigDecimal.valueOf(s.intervalDays()))
                        .setScale(0, RoundingMode.HALF_UP).intValueExact();
                interval = Math.max(interval, s.intervalDays() + 1); // always moves forward
            }
            interval = Math.min(interval, MAX_INTERVAL_DAYS);
            int miss = 5 - q;
            BigDecimal delta = new BigDecimal("0.1")
                    .subtract(BigDecimal.valueOf(miss).multiply(new BigDecimal("0.08")
                            .add(BigDecimal.valueOf(miss).multiply(new BigDecimal("0.02")))));
            BigDecimal ease = s.ease().add(delta).max(MIN_EASE).setScale(2, RoundingMode.HALF_UP);
            next = new State(repetitions, interval, ease, s.lapses());
        }
        return new Result(next, today.plusDays(next.intervalDays()));
    }
}
