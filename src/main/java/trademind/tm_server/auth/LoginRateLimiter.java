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

    /** Beyond this many tracked clients, new ones share one bucket instead of growing the map. */
    static final int DEFAULT_MAX_KEYS = 10_000;
    private static final String OVERFLOW_KEY = "\u0000overflow";

    private final int maxAttempts;
    private final long windowMillis;
    private final LongSupplier clock;
    private final int maxKeys;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(int maxAttempts, Duration window) {
        this(maxAttempts, window, System::currentTimeMillis, DEFAULT_MAX_KEYS);
    }

    LoginRateLimiter(int maxAttempts, Duration window, LongSupplier clock) {
        this(maxAttempts, window, clock, DEFAULT_MAX_KEYS);
    }

    LoginRateLimiter(int maxAttempts, Duration window, LongSupplier clock, int maxKeys) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = window.toMillis();
        this.clock = clock;
        this.maxKeys = maxKeys;
    }

    public Result check(String requested) {
        long now = clock.getAsLong();
        String key = bucketFor(requested, now);
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

    /**
     * Keeps memory bounded when many different addresses probe the login: expired windows are
     * dropped first, and if the table is still full a brand-new address shares one overflow bucket.
     */
    private String bucketFor(String key, long now) {
        if (windows.size() < maxKeys || windows.containsKey(key)) return key;
        windows.entrySet().removeIf(e -> e.getValue().resetAtMillis() <= now);
        return windows.size() < maxKeys ? key : OVERFLOW_KEY;
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
