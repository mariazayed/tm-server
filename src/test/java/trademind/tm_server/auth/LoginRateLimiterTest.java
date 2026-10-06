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
}
