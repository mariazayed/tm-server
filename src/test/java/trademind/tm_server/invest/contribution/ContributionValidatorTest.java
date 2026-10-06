package trademind.tm_server.invest.contribution;

import org.junit.jupiter.api.Test;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.dto.ContributionRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContributionValidatorTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-10-06");

    private static ContributionRequest request(String date, long cents, String shares, String price) {
        return new ContributionRequest(LocalDate.parse(date), cents, new BigDecimal(shares),
                new BigDecimal(price), 0L, null);
    }

    @Test
    void acceptsConsistentSharesAndPrice() {
        assertDoesNotThrow(() -> ContributionValidator.validate(request("2026-10-01", 30000, "2.55489022", "117.425"), TODAY));
    }

    @Test
    void acceptsBrokerRoundingWithinOnePercent() {
        // 300 / 117.425 = 2.5549; broker reports 2.55
        assertDoesNotThrow(() -> ContributionValidator.validate(request("2026-10-01", 30000, "2.55", "117.425"), TODAY));
    }

    @Test
    void rejectsSharesThatDoNotMatchAmountOverPrice() {
        assertThrows(BadRequestException.class,
                () -> ContributionValidator.validate(request("2026-10-01", 30000, "5", "117.425"), TODAY));
    }

    @Test
    void rejectsFutureDates() {
        assertThrows(BadRequestException.class,
                () -> ContributionValidator.validate(request("2026-10-07", 30000, "2.55489022", "117.425"), TODAY));
    }

    @Test
    void allowsToday() {
        assertDoesNotThrow(() -> ContributionValidator.validate(request("2026-10-06", 30000, "2.55489022", "117.425"), TODAY));
    }
}
