package trademind.tm_server.invest.performance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeSet;

import trademind.tm_server.invest.performance.PerformanceCalculator.Buy;
import trademind.tm_server.invest.performance.PerformanceCalculator.Point;
import trademind.tm_server.invest.performance.PerformanceCalculator.Result;

/**
 * The whole portfolio from several funds. Each fund is valued on its own with {@link PerformanceCalculator}, then the
 * funds are added up. With one fund the total is exactly that fund's result. No I/O, and all funds are taken to be in
 * the plan's one currency.
 */
public final class PortfolioCalculator {

    /** One fund's buys and the closes cached for its ticker. */
    public record Fund(Long id, String ticker, List<Buy> buys, NavigableMap<LocalDate, BigDecimal> closes) {
    }

    public record FundResult(Fund fund, Result result) {
    }

    /** @param total all funds added up; per-share fields (average cost, price) only when one fund holds anything */
    public record Portfolio(Result total, List<FundResult> funds) {
    }

    private PortfolioCalculator() {
    }

    public static Portfolio calculate(List<Fund> funds, LocalDate today) {
        List<FundResult> results = new ArrayList<>();
        for (Fund f : funds) {
            results.add(new FundResult(f, PerformanceCalculator.calculate(f.buys(), f.closes(), today)));
        }
        return new Portfolio(total(results), results);
    }

    private static Result total(List<FundResult> results) {
        List<Result> held = results.stream().map(FundResult::result).filter(r -> r.investedCents() > 0).toList();
        if (held.isEmpty()) {
            return new Result(0, 0, BigDecimal.ZERO, null, null, null, false, false, 0, 0, null, List.of());
        }
        if (held.size() == 1) return held.get(0); // identical to the single-fund numbers

        long invested = 0, fees = 0, value = 0;
        BigDecimal shares = BigDecimal.ZERO;
        LocalDate oldest = null;
        boolean fromCache = true, stale = false;
        for (Result r : held) {
            invested += r.investedCents();
            fees += r.feesCents();
            value += r.valueCents();
            shares = shares.add(r.shares());
            if (oldest == null || r.priceAsOf().isBefore(oldest)) oldest = r.priceAsOf();
            fromCache &= r.priceFromCache();
            stale |= r.stale();
        }
        BigDecimal pct = BigDecimal.valueOf(value - invested).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(invested), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
        // Shares of different funds cannot be averaged, and there is no single price, so those stay empty.
        return new Result(invested, fees, shares, null, null, oldest, fromCache, stale, value, value - invested, pct,
                combine(held));
    }

    /** One point per day any fund has a point; a fund counts as 0 before its first point and carries forward after. */
    static List<Point> combine(List<Result> held) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        for (Result r : held) r.series().forEach(p -> dates.add(p.date()));
        int[] next = new int[held.size()];
        long[] invested = new long[held.size()];
        long[] value = new long[held.size()];
        List<Point> out = new ArrayList<>();
        for (LocalDate d : dates) {
            long investedSum = 0, valueSum = 0;
            for (int i = 0; i < held.size(); i++) {
                List<Point> series = held.get(i).series();
                while (next[i] < series.size() && !series.get(next[i]).date().isAfter(d)) {
                    invested[i] = series.get(next[i]).investedCents();
                    value[i] = series.get(next[i]).valueCents();
                    next[i]++;
                }
                investedSum += invested[i];
                valueSum += value[i];
            }
            out.add(new Point(d, investedSum, valueSum));
        }
        return out;
    }
}
