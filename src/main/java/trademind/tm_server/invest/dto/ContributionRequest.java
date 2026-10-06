package trademind.tm_server.invest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param amountCents cash invested in cents, excluding the fee
 * @param shares      shares bought (fractions allowed)
 * @param price       per-share execution price
 * @param feeCents    broker fee in cents; defaults to 0
 */
public record ContributionRequest(
        @NotNull LocalDate contributedOn,
        @NotNull @Positive @Max(MAX_CENTS) Long amountCents,
        @NotNull @Positive @DecimalMax(MAX_SHARES) @Digits(integer = 12, fraction = 20) BigDecimal shares,
        @NotNull @Positive @DecimalMax(MAX_PRICE) @Digits(integer = 10, fraction = 20) BigDecimal price,
        @PositiveOrZero @Max(MAX_FEE_CENTS) Long feeCents,
        @Size(max = 2000) String notes) {

    /** Bounds that keep every stored value inside its column and every sum inside a long. */
    public static final long MAX_CENTS = 100_000_000_000L; // $1 billion per buy
    public static final long MAX_FEE_CENTS = 100_000_000L; // $1 million
    public static final String MAX_SHARES = "999999999999"; // numeric(20,8): 12 integer digits
    public static final String MAX_PRICE = "9999999999"; // numeric(14,4): 10 integer digits
}
