package trademind.tm_server.invest.price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.PriceQuote;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    /** Days re-fetched on every refresh so late corrections to recent closes are picked up. */
    static final int OVERLAP_DAYS = 5;

    /** @param error null on success; a failed refresh never touches the cached prices */
    public record RefreshResult(int fetched, int inserted, int updated, LocalDate latestDate, String error) {
    }

    private final PriceProvider provider;
    private final PriceQuoteRepository prices;
    private final InvestSettingsRepository settings;
    private final ContributionRepository contributions;
    private final Clock clock;

    public PriceService(PriceProvider provider, PriceQuoteRepository prices,
                        InvestSettingsRepository settings, ContributionRepository contributions, Clock clock) {
        this.provider = provider;
        this.prices = prices;
        this.settings = settings;
        this.contributions = contributions;
        this.clock = clock;
    }

    /**
     * Fetches from the day after the newest cached close (minus a small overlap), or, on first
     * run, from the earliest contribution or plan start. Safe to call repeatedly.
     */
    @Transactional
    public RefreshResult refresh() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        String ticker = s.getTicker();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        LocalDate latest = prices.findFirstByTickerOrderByPriceDateDesc(ticker)
                .map(PriceQuote::getPriceDate).orElse(null);
        LocalDate from = latest == null ? backfillStart(s) : latest.minusDays(OVERLAP_DAYS);
        if (from.isAfter(today)) {
            return new RefreshResult(0, 0, 0, latest, null);
        }

        List<PriceProvider.DailyClose> fetched;
        try {
            fetched = provider.fetchDailyCloses(ticker, from, today);
        } catch (PriceProvider.PriceProviderException e) {
            log.warn("Price refresh for {} failed, keeping cached prices: {}", ticker, e.getMessage());
            return new RefreshResult(0, 0, 0, latest, e.getMessage());
        }

        Map<LocalDate, PriceQuote> existing = new HashMap<>();
        for (PriceQuote q : prices.findByTickerAndPriceDateBetweenOrderByPriceDateAsc(ticker, from, today)) {
            existing.put(q.getPriceDate(), q);
        }
        int inserted = 0;
        int updated = 0;
        for (PriceProvider.DailyClose c : fetched) {
            PriceQuote q = existing.get(c.date());
            if (q == null) {
                prices.save(new PriceQuote(ticker, c.date(), c.close()));
                inserted++;
            } else if (q.getClose().compareTo(c.close()) != 0) {
                q.setClose(c.close());
                updated++;
            }
        }
        LocalDate newest = fetched.isEmpty() ? latest : fetched.get(fetched.size() - 1).date();
        if (latest != null && newest != null && latest.isAfter(newest)) newest = latest;
        return new RefreshResult(fetched.size(), inserted, updated, newest, null);
    }

    /** Cached closes for a ticker, oldest first, as a navigable map. */
    @Transactional(readOnly = true)
    public TreeMap<LocalDate, java.math.BigDecimal> cachedCloses(String ticker) {
        TreeMap<LocalDate, java.math.BigDecimal> map = new TreeMap<>();
        for (PriceQuote q : prices.findByTickerAndPriceDateBetweenOrderByPriceDateAsc(
                ticker, LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))) {
            map.put(q.getPriceDate(), q.getClose());
        }
        return map;
    }

    private LocalDate backfillStart(InvestSettings s) {
        LocalDate start = s.getStartDate();
        for (var c : contributions.findByDeletedAtIsNullOrderByContributedOnDesc()) {
            if (c.getContributedOn().isBefore(start)) start = c.getContributedOn();
        }
        return start.minusDays(OVERLAP_DAYS); // include the close just before the first buy
    }
}
