package trademind.tm_server.invest.holding;

import trademind.tm_server.invest.Holding;

import java.math.BigDecimal;

public record HoldingResponse(
        Long id,
        String ticker,
        String name,
        Holding.AssetClass assetClass,
        String region,
        BigDecimal targetPct,
        BigDecimal expenseRatio,
        String notes) {

    public static HoldingResponse from(Holding h) {
        return new HoldingResponse(h.getId(), h.getTicker(), h.getName(), h.getAssetClass(), h.getRegion(),
                h.getTargetPct(), h.getExpenseRatio(), h.getNotes());
    }
}
