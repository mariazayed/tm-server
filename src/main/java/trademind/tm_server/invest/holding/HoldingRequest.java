package trademind.tm_server.invest.holding;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import trademind.tm_server.invest.Holding;

import java.math.BigDecimal;

/**
 * @param ticker       as the price source knows it, e.g. VT or BRK-B
 * @param targetPct    the share of the portfolio you aim for, 0-100
 * @param expenseRatio yearly fund charge in percent, 0-5; left out when unknown
 */
public record HoldingRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9.\\-]{1,16}") String ticker,
        @Size(max = 120) String name,
        @NotNull Holding.AssetClass assetClass,
        @NotBlank @Size(max = 32) String region,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal targetPct,
        @DecimalMin("0") @DecimalMax("5") @Digits(integer = 1, fraction = 3) BigDecimal expenseRatio,
        @Size(max = 2000) String notes) {
}
