package trademind.tm_server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPropertiesTest {

    private static final String SECRET = "s".repeat(32);

    @Test
    void configuredOnlyWhenBothValuesPresent() {
        assertTrue(new AuthProperties("hash", SECRET, 14).configured());
        assertFalse(new AuthProperties(null, SECRET, 14).configured());
        assertFalse(new AuthProperties("hash", "", 14).configured());
        assertFalse(new AuthProperties(null, null, 14).configured());
    }

    @Test
    void rejectsWeakSecretAndBadLifetime() {
        assertThrows(IllegalArgumentException.class, () -> new AuthProperties("hash", "short", 14));
        assertThrows(IllegalArgumentException.class, () -> new AuthProperties("hash", SECRET, 0));
        assertThrows(IllegalArgumentException.class, () -> new AuthProperties("hash", SECRET, 91));
    }

    @Test
    void acceptsHashesFromPythonBcryptAndHtpasswd() {
        // Documented generation routes: python `bcrypt` ($2b$) and `htpasswd -B` ($2y$).
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        assertTrue(encoder.matches("correct horse battery",
                "$2b$12$2IIltHDJ7u4NsO92h.4Mk./RHXo5JPPYp9oVeNkFZ21sqXUghPZsK"));
        assertTrue(encoder.matches("correct horse battery",
                "$2y$12$2IIltHDJ7u4NsO92h.4Mk./RHXo5JPPYp9oVeNkFZ21sqXUghPZsK"));
    }
}
