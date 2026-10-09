package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiRateLimiterTest {

    /** A clock the test can move. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    @Test
    void allowsUpToTheLimitThenAsksToWait() {
        MutableClock clock = new MutableClock();
        AiRateLimiter limiter = new AiRateLimiter(3, Duration.ofMinutes(1), clock);
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.retryAfterSeconds());
            limiter.record();
            clock.advance(Duration.ofSeconds(10));
        }
        // First call was 30s ago, so a slot frees in 30s (reported rounded up).
        assertEquals(31, limiter.retryAfterSeconds());
    }

    @Test
    void theWindowSlides() {
        MutableClock clock = new MutableClock();
        AiRateLimiter limiter = new AiRateLimiter(1, Duration.ofMinutes(1), clock);
        limiter.record();
        clock.advance(Duration.ofSeconds(59));
        assertEquals(2, limiter.retryAfterSeconds());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(0, limiter.retryAfterSeconds());
    }

    @Test
    void checkingDoesNotUseASlot() {
        AiRateLimiter limiter = new AiRateLimiter(1, Duration.ofMinutes(1), new MutableClock());
        for (int i = 0; i < 5; i++) assertEquals(0, limiter.retryAfterSeconds());
    }
}
