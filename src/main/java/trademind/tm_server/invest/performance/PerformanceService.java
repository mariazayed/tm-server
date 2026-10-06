package trademind.tm_server.invest.performance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.price.PriceService;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PerformanceService {

    private final ContributionRepository contributions;
    private final InvestSettingsRepository settings;
    private final PriceService prices;
    private final Clock clock;

    public PerformanceService(ContributionRepository contributions, InvestSettingsRepository settings,
                              PriceService prices, Clock clock) {
        this.contributions = contributions;
        this.settings = settings;
        this.prices = prices;
        this.clock = clock;
    }

    /** @param range 3M, 6M, 1Y or ALL; only trims the chart series, totals always cover everything */
    public PerformanceResponse performance(String range) {
        String normalized = range == null ? "ALL" : range.toUpperCase(Locale.ROOT);
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        LocalDate cutoff = cutoff(normalized, today);

        List<PerformanceCalculator.Buy> buys = contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                .map(c -> new PerformanceCalculator.Buy(c.getContributedOn(), c.getAmountCents(), c.getShares(),
                        c.getPrice(), c.getFeeCents()))
                .toList();
        PerformanceCalculator.Result r =
                PerformanceCalculator.calculate(buys, prices.cachedCloses(s.getTicker()), today);

        List<PerformanceResponse.SeriesPoint> series = r.series().stream()
                .filter(p -> cutoff == null || !p.date().isBefore(cutoff))
                .map(p -> new PerformanceResponse.SeriesPoint(p.date(), p.investedCents(), p.valueCents()))
                .toList();
        return new PerformanceResponse(s.getTicker(), s.getCurrency(), r.investedCents(), r.feesCents(), r.shares(),
                r.averageCost(), r.currentPrice(), r.priceAsOf(), r.priceFromCache(), r.stale(), r.valueCents(),
                r.gainCents(), r.gainPercent(), normalized, series);
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
