package trademind.tm_server.invest.performance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.price.PriceService;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.HoldingRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PerformanceService {

    /** What the response calls the ticker when there is more than one fund. */
    public static final String MIXED = "MIXED";

    private final ContributionRepository contributions;
    private final InvestSettingsRepository settings;
    private final HoldingRepository holdings;
    private final PriceService prices;
    private final Clock clock;

    public PerformanceService(ContributionRepository contributions, InvestSettingsRepository settings,
                              HoldingRepository holdings, PriceService prices, Clock clock) {
        this.contributions = contributions;
        this.settings = settings;
        this.holdings = holdings;
        this.prices = prices;
        this.clock = clock;
    }

    /** @param range 3M, 6M, 1Y or ALL; only trims the chart series, totals always cover everything */
    public PerformanceResponse performance(String range) {
        String normalized = range == null ? "ALL" : range.toUpperCase(Locale.ROOT);
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        LocalDate cutoff = cutoff(normalized, today);

        List<Holding> funds = holdings.findByDeletedAtIsNullOrderByIdAsc();
        Map<Long, List<PerformanceCalculator.Buy>> buysByFund = new HashMap<>();
        for (Contribution c : contributions.findByDeletedAtIsNullOrderByContributedOnDesc()) {
            buysByFund.computeIfAbsent(c.getHolding().getId(), k -> new ArrayList<>())
                    .add(new PerformanceCalculator.Buy(c.getContributedOn(), c.getAmountCents(), c.getShares(),
                            c.getPrice(), c.getFeeCents()));
        }
        List<PortfolioCalculator.Fund> inputs = new ArrayList<>();
        for (Holding h : funds) {
            inputs.add(new PortfolioCalculator.Fund(h.getId(), h.getTicker(),
                    buysByFund.getOrDefault(h.getId(), List.of()), prices.cachedCloses(h.getTicker())));
        }
        PortfolioCalculator.Portfolio portfolio = PortfolioCalculator.calculate(inputs, today);
        PerformanceCalculator.Result r = portfolio.total();

        List<HoldingPerformance> perFund = new ArrayList<>();
        for (int i = 0; i < funds.size(); i++) {
            perFund.add(holding(funds.get(i), portfolio.funds().get(i).result(), r.valueCents(), cutoff));
        }
        String ticker = funds.size() == 1 ? funds.get(0).getTicker() : funds.isEmpty() ? s.getTicker() : MIXED;
        return new PerformanceResponse(ticker, s.getCurrency(), r.investedCents(), r.feesCents(), r.shares(),
                r.averageCost(), r.currentPrice(), r.priceAsOf(), r.priceFromCache(), r.stale(), r.valueCents(),
                r.gainCents(), r.gainPercent(), normalized, trim(r.series(), cutoff), perFund);
    }

    private static HoldingPerformance holding(Holding h, PerformanceCalculator.Result r, long portfolioValue,
                                              LocalDate cutoff) {
        BigDecimal allocation = null;
        BigDecimal drift = null;
        if (portfolioValue > 0) {
            allocation = BigDecimal.valueOf(r.valueCents()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(portfolioValue), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
            drift = allocation.subtract(h.getTargetPct());
        }
        return new HoldingPerformance(h.getId(), h.getTicker(), h.getName(), h.getAssetClass().name(), h.getTargetPct(),
                r.investedCents(), r.feesCents(), r.shares(), r.averageCost(), r.currentPrice(), r.priceAsOf(),
                r.priceFromCache(), r.stale(), r.valueCents(), r.gainCents(), r.gainPercent(), allocation, drift,
                trim(r.series(), cutoff));
    }

    private static List<PerformanceResponse.SeriesPoint> trim(List<PerformanceCalculator.Point> series, LocalDate cutoff) {
        return series.stream()
                .filter(p -> cutoff == null || !p.date().isBefore(cutoff))
                .map(p -> new PerformanceResponse.SeriesPoint(p.date(), p.investedCents(), p.valueCents()))
                .toList();
    }

    private static LocalDate cutoff(String range, LocalDate today) {
        return switch (range) {
            case "ALL" -> null;
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            default -> throw new BadRequestException("range must be one of 3M, 6M, 1Y, ALL");
        };
    }
}
