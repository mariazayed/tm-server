package trademind.tm_server.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;

/**
 * Single-user auth settings for the investing tracker. Both secrets are optional so the
 * existing trade-journal API keeps starting without them; when they are missing, login is
 * disabled and every protected route stays closed (fail closed, never open).
 *
 * @param passwordHash  BCrypt hash of the owner's password (generate with {@code PasswordHashTool})
 * @param sessionSecret HMAC secret used to sign session tokens, at least 32 bytes
 * @param sessionDays   how long an issued token stays valid
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String passwordHash,
        String sessionSecret,
        @DefaultValue("14") int sessionDays) {

    public static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (sessionDays < 1 || sessionDays > 90) {
            throw new IllegalArgumentException("app.auth.session-days must be between 1 and 90");
        }
        if (isPresent(sessionSecret)
                && sessionSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.auth.session-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
    }

    /** True only when both a password hash and a signing secret are configured. */
    public boolean configured() {
        return isPresent(passwordHash) && isPresent(sessionSecret);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
