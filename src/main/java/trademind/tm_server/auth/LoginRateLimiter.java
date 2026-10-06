package trademind.tm_server.auth;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Fixed-window in-memory limiter; enough for a single-instance personal app. */
public class LoginRateLimiter {

    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private record Window(int count, long resetAtMillis) {
    }

    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(int maxAttempts, Duration window) {
        this(maxAttempts, window, System::currentTimeMillis);
    }

    LoginRateLimiter(int maxAttempts, Duration window, LongSupplier clock) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public Result check(String key) {
        long now = clock.getAsLong();
        Window updated = windows.compute(key, (k, w) ->
                (w == null || w.resetAtMillis() <= now)
                        ? new Window(1, now + windowMillis)
                        : new Window(w.count() + 1, w.resetAtMillis()));
        if (updated.count() > maxAttempts) {
            long retry = (long) Math.ceil((updated.resetAtMillis() - now) / 1000.0);
            return new Result(false, retry);
        }
        return new Result(true, 0);
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
