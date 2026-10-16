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

    /**
     * Rounds shares to 8 and price to 4 decimals, the precision the database keeps, so what is
     * validated is exactly what is stored (and a 5-decimal price from a broker is not an error).
     */
    public static ContributionRequest normalize(ContributionRequest r) {
        return new ContributionRequest(r.contributedOn(), r.amountCents(),
                r.shares().setScale(8, java.math.RoundingMode.HALF_UP),
                r.price().setScale(4, java.math.RoundingMode.HALF_UP), r.feeCents(), r.notes(), r.holdingId(), r.ticker());
    }

    public static void validate(ContributionRequest r, LocalDate today) {
        validate(r, today, false);
    }

    public static void validate(ContributionRequest r, LocalDate today, boolean mayUsePriorCash) {
        // The same bounds as the request annotations: CSV rows never pass through bean validation.
        if (r.amountCents() > ContributionRequest.MAX_CENTS
                || (r.feeCents() != null && r.feeCents() > ContributionRequest.MAX_FEE_CENTS)
                || r.shares().compareTo(new BigDecimal(ContributionRequest.MAX_SHARES)) > 0
                || r.price().compareTo(new BigDecimal(ContributionRequest.MAX_PRICE)) > 0) {
            throw new BadRequestException("Amount, fee, shares or price is unrealistically large");
        }
        if (r.contributedOn().isAfter(today)) {
            throw new BadRequestException("Contribution date cannot be in the future");
        }
        BigDecimal cash = BigDecimal.valueOf(r.amountCents()).movePointLeft(2);
        BigDecimal expectedShares = cash.divide(r.price(), MathContext.DECIMAL64);
        // Whole-share purchases can use cash left from earlier contributions;
        // the service checks that the running cash balance covers the purchase.
        if (r.shares().stripTrailingZeros().scale() <= 0
                && (mayUsePriorCash || r.shares().compareTo(expectedShares) <= 0)) {
            return;
        }
        BigDecimal gap = r.shares().subtract(expectedShares).abs()
                .divide(expectedShares, MathContext.DECIMAL64);
        if (gap.compareTo(TOLERANCE) > 0) {
            throw new BadRequestException(
                    "Shares do not match amount / price (expected about " + expectedShares
                            .setScale(4, java.math.RoundingMode.HALF_UP).toPlainString() + ")");
        }
    }
}
