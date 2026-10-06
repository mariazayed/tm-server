package trademind.tm_server.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    @Test
    void blocksAfterMaxAttemptsAndRecoversAfterWindow() {
        AtomicLong now = new AtomicLong(0);
        LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofSeconds(1), now::get);

        assertTrue(limiter.check("a").allowed());
        assertTrue(limiter.check("a").allowed());
        LoginRateLimiter.Result blocked = limiter.check("a");
        assertFalse(blocked.allowed());
        assertEquals(1, blocked.retryAfterSeconds());
        assertTrue(limiter.check("b").allowed());

        now.set(1001);
        assertTrue(limiter.check("a").allowed());
    }

    @Test
    void resetClearsAKey() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofSeconds(1), () -> 0);
        limiter.check("a");
        limiter.reset("a");
        assertTrue(limiter.check("a").allowed());
    }

    @Test
    void memoryStaysBoundedWhenManyAddressesProbe() {
        AtomicLong now = new AtomicLong(0);
        LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofSeconds(60), now::get, 3);

        // Three real addresses fill the table.
        assertTrue(limiter.check("a").allowed());
        assertTrue(limiter.check("b").allowed());
        assertTrue(limiter.check("c").allowed());

        // Further new addresses all land in one shared bucket, so they cannot grow memory ...
        assertTrue(limiter.check("x1").allowed());
        assertTrue(limiter.check("x2").allowed());
        // ... and a flood from many addresses gets throttled together.
        assertFalse(limiter.check("x3").allowed());

        // Known addresses keep their own counters.
        assertTrue(limiter.check("a").allowed());
        assertFalse(limiter.check("a").allowed());
    }

    @Test
    void expiredWindowsAreReclaimedBeforeSharingABucket() {
        AtomicLong now = new AtomicLong(0);
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofSeconds(1), now::get, 2);
        limiter.check("a");
        limiter.check("b");
        now.set(5_000); // both windows have expired
        assertTrue(limiter.check("c").allowed()); // gets its own slot, not the overflow bucket
        assertFalse(limiter.check("c").allowed());
        assertTrue(limiter.check("d").allowed());
    }
}
