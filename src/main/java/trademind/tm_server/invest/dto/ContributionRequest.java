package trademind.tm_server.invest.dto;

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
        @NotNull @Positive Long amountCents,
        @NotNull @Positive BigDecimal shares,
        @NotNull @Positive BigDecimal price,
        @PositiveOrZero Long feeCents,
        @Size(max = 2000) String notes) {
}
