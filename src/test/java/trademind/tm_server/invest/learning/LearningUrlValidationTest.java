package trademind.tm_server.invest.learning;

import org.junit.jupiter.api.Test;
import trademind.tm_server.exceptions.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningUrlValidationTest {

    @Test
    void acceptsHttpAndHttpsAndTrims() {
        assertEquals("https://example.com/a?b=1", LearningService.validUrl("  https://example.com/a?b=1 "));
        assertEquals("http://example.com", LearningService.validUrl("http://example.com"));
    }

    @Test
    void blankMeansNoLink() {
        assertNull(LearningService.validUrl(null));
        assertNull(LearningService.validUrl("   "));
    }

    @Test
    void rejectsDangerousOrMalformedLinks() {
        for (String bad : new String[] {"javascript:alert(1)", "file:///etc/passwd", "ftp://example.com",
                "data:text/html,<script>", "example.com", "http://", "not a url", "//example.com"}) {
            assertThrows(BadRequestException.class, () -> LearningService.validUrl(bad), bad);
        }
    }
}
