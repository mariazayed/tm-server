package trademind.tm_server.invest.contribution;

import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.dto.ContributionRequest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

/** Business rules beyond bean validation, shared by the API and CSV import. */
public final class ContributionValidator {

    /** Allowed relative gap between shares and amount / price (broker rounding). */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private ContributionValidator() {
    }

    public static void validate(ContributionRequest r, LocalDate today) {
        if (r.contributedOn().isAfter(today)) {
            throw new BadRequestException("Contribution date cannot be in the future");
        }
        BigDecimal cash = BigDecimal.valueOf(r.amountCents()).movePointLeft(2);
        BigDecimal expectedShares = cash.divide(r.price(), MathContext.DECIMAL64);
        BigDecimal gap = r.shares().subtract(expectedShares).abs()
                .divide(expectedShares, MathContext.DECIMAL64);
        if (gap.compareTo(TOLERANCE) > 0) {
            throw new BadRequestException(
                    "Shares do not match amount / price (expected about " + expectedShares
                            .setScale(4, java.math.RoundingMode.HALF_UP).toPlainString() + ")");
        }
    }
}
