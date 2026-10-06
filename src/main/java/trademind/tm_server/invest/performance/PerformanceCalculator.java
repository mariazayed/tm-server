package trademind.tm_server.invest.performance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeSet;

/**
 * Portfolio math, no I/O. All cash is integer cents; shares and prices stay BigDecimal.
 *
 * <p>"Invested" is cash out of pocket: amounts plus fees. The average cost therefore includes fees.
 */
public final class PerformanceCalculator {

    /** A close older than this many days (relative to today) is flagged stale. */
    public static final int STALE_AFTER_DAYS = 4;

    public record Buy(LocalDate date, long amountCents, BigDecimal shares, BigDecimal price, long feeCents) {
    }

    public record Point(LocalDate date, long investedCents, long valueCents) {
    }

    /**
     * @param averageCost      invested / shares, 4 decimals; null with no shares
     * @param currentPrice     latest known close (or the last execution price when none is cached)
     * @param priceAsOf        date of {@code currentPrice}
     * @param gainPercent      gain / invested * 100, 2 decimals; null when nothing is invested
     * @param priceFromCache   false when {@code currentPrice} fell back to your own last buy
     * @param stale            the price is more than {@value #STALE_AFTER_DAYS} days old
     */
    public record Result(long investedCents, long feesCents, BigDecimal shares, BigDecimal averageCost,
                         BigDecimal currentPrice, LocalDate priceAsOf, boolean priceFromCache, boolean stale,
                         long valueCents, long gainCents, BigDecimal gainPercent, List<Point> series) {
    }

    private PerformanceCalculator() {
    }

    public static Result calculate(List<Buy> buys, NavigableMap<LocalDate, BigDecimal> closes, LocalDate today) {
        List<Buy> sorted = new ArrayList<>(buys);
        sorted.sort(Comparator.comparing(Buy::date));

        long invested = 0;
        long fees = 0;
        BigDecimal shares = BigDecimal.ZERO;
        for (Buy b : sorted) {
            invested += b.amountCents() + b.feeCents();
            fees += b.feeCents();
            shares = shares.add(b.shares());
        }
        if (sorted.isEmpty()) {
            return new Result(0, 0, BigDecimal.ZERO, null, null, null, false, false, 0, 0, null, List.of());
        }

        BigDecimal price;
        LocalDate asOf;
        boolean fromCache = !closes.isEmpty();
        if (fromCache) {
            var last = closes.lastEntry();
            price = last.getValue();
            asOf = last.getKey();
        } else {
            Buy last = sorted.get(sorted.size() - 1);
            price = last.price();
            asOf = last.date();
        }
        boolean stale = ChronoUnit.DAYS.between(asOf, today) > STALE_AFTER_DAYS;

        long value = valueCents(shares, price);
        BigDecimal avg = shares.signum() > 0
                ? BigDecimal.valueOf(invested).movePointLeft(2).divide(shares, 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal pct = invested > 0
                ? BigDecimal.valueOf(value - invested).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(invested), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new Result(invested, fees, shares, avg, price, asOf, fromCache, stale, value, value - invested,
                pct, series(sorted, closes));
    }

    /**
     * One point per cached trading day from the first buy onward. Before any prices exist the
     * series falls back to one point per buy date, valued at that buy's execution price, so the
     * chart still has something honest to draw.
     */
    static List<Point> series(List<Buy> sorted, NavigableMap<LocalDate, BigDecimal> closes) {
        LocalDate first = sorted.get(0).date();
        TreeSet<LocalDate> dates = new TreeSet<>();
        closes.tailMap(first, true).keySet().forEach(dates::add);
        sorted.forEach(b -> dates.add(b.date()));

        List<Point> points = new ArrayList<>();
        int next = 0;
        long invested = 0;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal lastExecution = sorted.get(0).price();
        for (LocalDate d : dates) {
            while (next < sorted.size() && !sorted.get(next).date().isAfter(d)) {
                Buy b = sorted.get(next++);
                invested += b.amountCents() + b.feeCents();
                shares = shares.add(b.shares());
                lastExecution = b.price();
            }
            var close = closes.floorEntry(d);
            BigDecimal price = close != null && !close.getKey().isBefore(first) ? close.getValue() : lastExecution;
            points.add(new Point(d, invested, valueCents(shares, price)));
        }
        return points;
    }

    private static long valueCents(BigDecimal shares, BigDecimal price) {
        return shares.multiply(price).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
