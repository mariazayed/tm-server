package trademind.tm_server.invest.performance;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.performance.PerformanceCalculator.Buy;
import trademind.tm_server.invest.performance.PerformanceCalculator.Point;
import trademind.tm_server.invest.performance.PerformanceCalculator.Result;
import trademind.tm_server.invest.performance.PortfolioCalculator.Fund;
import trademind.tm_server.invest.performance.PortfolioCalculator.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioCalculatorTest {

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    private static TreeMap<LocalDate, BigDecimal> closes(String... dateAndClose) {
        TreeMap<LocalDate, BigDecimal> m = new TreeMap<>();
        for (int i = 0; i < dateAndClose.length; i += 2) m.put(d(dateAndClose[i]), n(dateAndClose[i + 1]));
        return m;
    }

    private static final LocalDate TODAY = d("2026-10-15");

    /** Fund A: 3 shares on 08-01 and 2.5 on 09-01. Prices 100 -> 130. */
    private static Fund a() {
        return new Fund(1L, "AAA", List.of(new Buy(d("2026-08-01"), 30000, n("3"), n("100"), 0),
                new Buy(d("2026-09-01"), 30000, n("2.5"), n("120"), 100)),
                closes("2026-08-01", "100", "2026-08-15", "110", "2026-09-01", "120", "2026-10-14", "130"));
    }

    /** Fund B: bought only on 09-15, 10 shares at 20. Prices 20 -> 22. */
    private static Fund b() {
        return new Fund(2L, "BBB", List.of(new Buy(d("2026-09-15"), 20000, n("10"), n("20"), 0)),
                closes("2026-09-15", "20", "2026-10-10", "22"));
    }

    @Test
    void oneFundGivesExactlyTheSingleFundNumbers() {
        Fund a = a();
        Result alone = PerformanceCalculator.calculate(a.buys(), a.closes(), TODAY);
        Portfolio p = PortfolioCalculator.calculate(List.of(a), TODAY);
        assertEquals(alone, p.total());
        assertEquals(1, p.funds().size());
    }

    @Test
    void aFundWithNoBuysChangesNothing() {
        Fund empty = new Fund(3L, "CCC", List.of(), closes("2026-10-01", "50"));
        Result alone = PerformanceCalculator.calculate(a().buys(), a().closes(), TODAY);
        Portfolio p = PortfolioCalculator.calculate(List.of(a(), empty), TODAY);
        assertEquals(alone, p.total());
        assertEquals(2, p.funds().size());
        assertEquals(0, p.funds().get(1).result().valueCents());
    }

    @Test
    void twoFundsAreAddedUp() {
        Portfolio p = PortfolioCalculator.calculate(List.of(a(), b()), TODAY);
        Result t = p.total();
        // A: invested 30000 + 30100 = 60100; 5.5 shares * 130 = 71500.  B: invested 20000; 10 shares * 22 = 22000.
        assertEquals(80100, t.investedCents());
        assertEquals(100, t.feesCents());
        assertEquals(93500, t.valueCents());
        assertEquals(13400, t.gainCents());
        assertEquals(new BigDecimal("16.73"), t.gainPercent()); // 13400 / 80100
        assertEquals(n("15.5"), t.shares());
        // shares of different funds cannot be averaged, and there is no single price
        assertNull(t.averageCost());
        assertNull(t.currentPrice());
        assertEquals(d("2026-10-10"), t.priceAsOf(), "the oldest of the funds' latest closes");
        assertTrue(t.priceFromCache());
    }

    @Test
    void theTotalIsStaleWhenAnyFundIs() {
        Fund old = new Fund(2L, "BBB", b().buys(), closes("2026-09-15", "20", "2026-09-20", "21"));
        Fund fresh = new Fund(2L, "BBB", b().buys(), closes("2026-09-15", "20", "2026-10-13", "22"));
        assertTrue(PortfolioCalculator.calculate(List.of(a(), old), TODAY).total().stale());
        assertFalse(PortfolioCalculator.calculate(List.of(a(), fresh), TODAY).total().stale());
    }

    @Test
    void aFundWithoutCachedPricesFallsBackToItsLastBuyAndSaysSo() {
        Fund noPrices = new Fund(2L, "BBB", b().buys(), new TreeMap<>());
        Result t = PortfolioCalculator.calculate(List.of(a(), noPrices), TODAY).total();
        assertFalse(t.priceFromCache());
        assertEquals(71500 + 20000, t.valueCents(), "B valued at its own buy price of 20");
    }

    @Test
    void theSeriesAddsFundsOnEveryDayAnyFundHasAPoint() {
        List<Point> series = PortfolioCalculator.calculate(List.of(a(), b()), TODAY).total().series();
        // before B exists the portfolio is just A
        Point aug15 = series.stream().filter(x -> x.date().equals(d("2026-08-15"))).findFirst().orElseThrow();
        assertEquals(30000, aug15.investedCents());
        assertEquals(33000, aug15.valueCents()); // 3 * 110
        // on B's first buy day both are in: A 5.5 shares at 120 = 66000, B 10 at 20 = 20000
        Point sep15 = series.stream().filter(x -> x.date().equals(d("2026-09-15"))).findFirst().orElseThrow();
        assertEquals(60100 + 20000, sep15.investedCents());
        assertEquals(66000 + 20000, sep15.valueCents());
        // A has no point on 10-10 but its last value carries forward next to B's new close
        Point oct10 = series.stream().filter(x -> x.date().equals(d("2026-10-10"))).findFirst().orElseThrow();
        assertEquals(66000 + 22000, oct10.valueCents());
        // the end of the series is the total
        Point last = series.get(series.size() - 1);
        assertEquals(93500, last.valueCents());
        assertEquals(80100, last.investedCents());
        // dates are strictly increasing, one point per day
        for (int i = 1; i < series.size(); i++) assertTrue(series.get(i).date().isAfter(series.get(i - 1).date()));
    }

    @Test
    void nothingHeldIsEmptyNotAnError() {
        Result t = PortfolioCalculator.calculate(List.of(new Fund(1L, "AAA", List.of(), closes("2026-10-01", "5"))), TODAY).total();
        assertEquals(0, t.valueCents());
        assertNull(t.gainPercent());
        assertTrue(t.series().isEmpty());
        assertNotNull(t.shares());
        assertTrue(PortfolioCalculator.calculate(List.of(), TODAY).total().series().isEmpty());
    }
}
