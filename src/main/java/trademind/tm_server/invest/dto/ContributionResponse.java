package trademind.tm_server.invest.dto;

import trademind.tm_server.invest.Contribution;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContributionResponse(
        Long id,
        LocalDate contributedOn,
        long amountCents,
        BigDecimal shares,
        BigDecimal price,
        long feeCents,
        String notes) {

    public static ContributionResponse from(Contribution c) {
        return new ContributionResponse(c.getId(), c.getContributedOn(), c.getAmountCents(),
                c.getShares(), c.getPrice(), c.getFeeCents(), c.getNotes());
    }
}
