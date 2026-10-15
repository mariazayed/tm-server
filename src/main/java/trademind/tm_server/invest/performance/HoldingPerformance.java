package trademind.tm_server.invest.performance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * How one fund is doing, and where it stands against its target.
 *
 * @param allocationPct its share of the portfolio's value now, 2 decimals; null when nothing is held
 * @param driftPoints   allocation minus target in percentage points; null when nothing is held
 */
public record HoldingPerformance(
        Long holdingId,
        String ticker,
        String name,
        String assetClass,
        BigDecimal targetPct,
        long investedCents,
        long feesCents,
        BigDecimal shares,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        LocalDate priceAsOf,
        boolean priceFromCache,
        boolean stale,
        long valueCents,
        long gainCents,
        BigDecimal gainPercent,
        BigDecimal allocationPct,
        BigDecimal driftPoints,
        List<PerformanceResponse.SeriesPoint> series) {
}
