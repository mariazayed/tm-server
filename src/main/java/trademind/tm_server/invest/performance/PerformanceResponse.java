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
        List<SeriesPoint> series,
        List<HoldingPerformance> holdings) {

    /** The response as it was before there were several funds: no per-fund breakdown. */
    public PerformanceResponse(String ticker, String currency, long investedCents, long feesCents, BigDecimal shares,
                               BigDecimal averageCost, BigDecimal currentPrice, LocalDate priceAsOf,
                               boolean priceFromCache, boolean stale, long valueCents, long gainCents,
                               BigDecimal gainPercent, String range, List<SeriesPoint> series) {
        this(ticker, currency, investedCents, feesCents, shares, averageCost, currentPrice, priceAsOf, priceFromCache,
                stale, valueCents, gainCents, gainPercent, range, series, List.of());
    }

    public record SeriesPoint(LocalDate date, long investedCents, long valueCents) {
    }
}
