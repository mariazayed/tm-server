package trademind.tm_server.invest.performance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PerformanceResponse(
        String ticker,
        String currency,
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
        String range,
        List<SeriesPoint> series) {

    public record SeriesPoint(LocalDate date, long investedCents, long valueCents) {
    }
}
