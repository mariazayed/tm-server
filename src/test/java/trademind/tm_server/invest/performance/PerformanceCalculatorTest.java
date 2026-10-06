package trademind.tm_server.invest.performance;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.performance.PerformanceCalculator.Buy;
import trademind.tm_server.invest.performance.PerformanceCalculator.Point;
import trademind.tm_server.invest.performance.PerformanceCalculator.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceCalculatorTest {

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    private static final List<Buy> TWO_BUYS = List.of(
            new Buy(d("2026-08-01"), 30000, n("3"), n("100"), 0),
            new Buy(d("2026-09-01"), 30000, n("2.5"), n("120"), 100));

    private static TreeMap<LocalDate, BigDecimal> closes() {
        TreeMap<LocalDate, BigDecimal> m = new TreeMap<>();
        m.put(d("2026-08-01"), n("100"));
        m.put(d("2026-08-15"), n("110"));
        m.put(d("2026-09-01"), n("120"));
        m.put(d("2026-09-10"), n("130"));
        return m;
    }

    @Test
    void totalsMatchAHandCalculation() {
        Result r = PerformanceCalculator.calculate(TWO_BUYS, closes(), d("2026-09-11"));

        assertEquals(60100, r.investedCents());      // 300 + 300 + 1.00 fee
        assertEquals(100, r.feesCents());
        assertEquals(0, n("5.5").compareTo(r.shares()));
        assertEquals(n("109.2727"), r.averageCost()); // 601.00 / 5.5
        assertEquals(n("130"), r.currentPrice());
        assertEquals(d("2026-09-10"), r.priceAsOf());
        assertEquals(71500, r.valueCents());          // 5.5 * 130
        assertEquals(11400, r.gainCents());
        assertEquals(n("18.97"), r.gainPercent());    // 114.00 / 601.00
        assertTrue(r.priceFromCache());
        assertFalse(r.stale());
    }

    @Test
    void seriesAccumulatesSharesAndInvestedOnEachPriceDay() {
        List<Point> s = PerformanceCalculator.calculate(TWO_BUYS, closes(), d("2026-09-11")).series();
        assertEquals(List.of(
                new Point(d("2026-08-01"), 30000, 30000),
                new Point(d("2026-08-15"), 30000, 33000),
                new Point(d("2026-09-01"), 60100, 66000),
                new Point(d("2026-09-10"), 60100, 71500)), s);
    }

    @Test
    void aBuyOnANonTradingDayGetsItsOwnPointValuedAtTheLastClose() {
        List<Buy> buys = List.of(new Buy(d("2026-08-16"), 11000, n("1"), n("110"), 0));
        List<Point> s = PerformanceCalculator.calculate(buys, closes(), d("2026-09-11")).series();
        assertEquals(new Point(d("2026-08-16"), 11000, 11000), s.get(0)); // carries the 08-15 close of 110
        assertEquals(new Point(d("2026-09-10"), 11000, 13000), s.get(s.size() - 1));
    }

    @Test
    void withoutCachedPricesFallsBackToYourExecutionPrices() {
        Result r = PerformanceCalculator.calculate(TWO_BUYS, new TreeMap<>(), d("2026-09-02"));
        assertFalse(r.priceFromCache());
        assertEquals(n("120"), r.currentPrice());
        assertEquals(d("2026-09-01"), r.priceAsOf());
        assertEquals(List.of(new Point(d("2026-08-01"), 30000, 30000), new Point(d("2026-09-01"), 60100, 66000)),
                r.series());
    }

    @Test
    void flagsOldPricesAsStale() {
        assertTrue(PerformanceCalculator.calculate(TWO_BUYS, closes(), d("2026-09-15")).stale());
        assertFalse(PerformanceCalculator.calculate(TWO_BUYS, closes(), d("2026-09-14")).stale());
    }

    @Test
    void closesBeforeTheFirstBuyAreIgnoredInTheSeries() {
        TreeMap<LocalDate, BigDecimal> m = closes();
        m.put(d("2026-07-20"), n("90"));
        List<Point> s = PerformanceCalculator.calculate(TWO_BUYS, m, d("2026-09-11")).series();
        assertEquals(d("2026-08-01"), s.get(0).date());
    }

    @Test
    void noContributionsGivesZerosAndNoPercent() {
        Result r = PerformanceCalculator.calculate(List.of(), closes(), d("2026-09-11"));
        assertEquals(0, r.investedCents());
        assertEquals(0, r.valueCents());
        assertNull(r.gainPercent());
        assertNull(r.averageCost());
        assertTrue(r.series().isEmpty());
    }

    @Test
    void aLossIsNegative() {
        TreeMap<LocalDate, BigDecimal> m = new TreeMap<>();
        m.put(d("2026-08-01"), n("90"));
        Result r = PerformanceCalculator.calculate(
                List.of(new Buy(d("2026-08-01"), 30000, n("3"), n("100"), 0)), m, d("2026-08-02"));
        assertEquals(27000, r.valueCents());
        assertEquals(-3000, r.gainCents());
        assertEquals(n("-10.00"), r.gainPercent());
    }
}
