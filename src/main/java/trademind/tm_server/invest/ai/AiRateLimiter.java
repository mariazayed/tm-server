package trademind.tm_server.invest.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/** At most {@code limit} calls in any rolling window. In memory and single instance; resets on restart. */
public class AiRateLimiter {

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Deque<Instant> calls = new ArrayDeque<>();

    public AiRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** Seconds to wait before a call would be allowed, or 0 when it is allowed now (without counting it). */
    public synchronized long retryAfterSeconds() {
        Instant now = clock.instant();
        prune(now);
        if (calls.size() < limit) return 0;
        Instant frees = calls.peekFirst().plus(window);
        return Math.max(1, Duration.between(now, frees).toSeconds() + 1);
    }

    /** Counts a call. Callers check {@link #retryAfterSeconds()} first, under the same lock as the daily cap. */
    public synchronized void record() {
        Instant now = clock.instant();
        prune(now);
        calls.addLast(now);
    }

    private void prune(Instant now) {
        Instant cutoff = now.minus(window);
        while (!calls.isEmpty() && !calls.peekFirst().isAfter(cutoff)) {
            calls.removeFirst();
        }
    }
}
