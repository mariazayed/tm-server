package trademind.tm_server.trade;

import org.junit.jupiter.api.Test;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.enums.SessionEnum;
import trademind.tm_server.enums.TradeTypeEnum;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Exercises the pure calculation methods directly (no Spring context, no
// mocking) -- these are the package-private statics TradeEnrichmentService
// delegates to once it has resolved an Asset's contractSize. Deliberately
// covers the divide-by-zero guards the enrichment plan called for.
class TradeEnrichmentServiceTest {

    @Test
    void dayOfWeekFor_mapsJavaDayOfWeekToEnum() {
        // 2026-09-09 is a Wednesday.
        LocalDateTime wednesday = LocalDateTime.of(2026, 9, 9, 10, 0);
        assertEquals(DayOfWeekEnum.WEDNESDAY, TradeEnrichmentService.dayOfWeekFor(wednesday));
    }

    @Test
    void sessionFor_bucketsByUtcHour() {
        assertEquals(SessionEnum.ASIAN_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 3, 0)));
        assertEquals(SessionEnum.ASIAN_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 7, 59)));
        assertEquals(SessionEnum.LONDON_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 8, 0)));
        assertEquals(SessionEnum.LONDON_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 12, 59)));
        assertEquals(SessionEnum.NEW_YORK_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 13, 0)));
        assertEquals(SessionEnum.NEW_YORK_SESSION, TradeEnrichmentService.sessionFor(LocalDateTime.of(2026, 9, 9, 23, 0)));
    }

    @Test
    void tradeTypeFor_classifiesByDuration() {
        LocalDateTime entry = LocalDateTime.of(2026, 9, 9, 10, 0);
        assertEquals(TradeTypeEnum.SCALPING, TradeEnrichmentService.tradeTypeFor(entry, entry.plusMinutes(5)));
        assertEquals(TradeTypeEnum.INTRADAY, TradeEnrichmentService.tradeTypeFor(entry, entry.plusHours(2)));
        assertEquals(TradeTypeEnum.SWING, TradeEnrichmentService.tradeTypeFor(entry, entry.plusDays(2)));
    }

    @Test
    void tradeTypeFor_returnsNullForNegativeDuration() {
        // Bad data (exit before entry) -- don't guess a classification for it.
        LocalDateTime entry = LocalDateTime.of(2026, 9, 9, 10, 0);
        assertNull(TradeEnrichmentService.tradeTypeFor(entry, entry.minusMinutes(5)));
    }

    @Test
    void riskAmountFor_computesNotionalRisk() {
        // 50-pip stop (0.0050), 1.0 lot, 100000 contract size -> $500 risk.
        assertEquals(500.0, TradeEnrichmentService.riskAmountFor(1.1000, 1.0950, 1.0, 100000), 0.0001);
    }

    @Test
    void riskPercentFor_computesPercentOfBalance() {
        assertEquals(5.0, TradeEnrichmentService.riskPercentFor(500.0, 10000.0), 0.0001);
    }

    @Test
    void riskRewardFor_computesRatio() {
        // 50-pip stop, 100-pip target -> 1:2.
        assertEquals(2.0, TradeEnrichmentService.riskRewardFor(1.1000, 1.0950, 1.1100), 0.0001);
    }

    @Test
    void riskRewardFor_guardsZeroStopDistance() {
        // entryPrice == slPrice: no stop distance to compare a target against.
        assertNull(TradeEnrichmentService.riskRewardFor(1.1000, 1.1000, 1.1100));
    }
}
