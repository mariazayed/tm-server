package trademind.tm_server.invest.price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.PriceQuote;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.HoldingRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final HoldingRepository holdings;
    private final ContributionRepository contributions;
    private final Clock clock;

    public PriceService(PriceProvider provider, PriceQuoteRepository prices,
                        InvestSettingsRepository settings, HoldingRepository holdings,
                        ContributionRepository contributions, Clock clock) {
        this.provider = provider;
        this.prices = prices;
        this.settings = settings;
        this.holdings = holdings;
        this.contributions = contributions;
        this.clock = clock;
    }

    /**
     * Refreshes every fund, one after the other. A fund that fails keeps its cached prices and the others still
     * update; the error names each fund that failed. The reported date is the oldest newest-close among the funds,
     * the day every fund is priced through.
     */
    @Transactional
    public RefreshResult refresh() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        int fetched = 0, inserted = 0, updated = 0;
        LocalDate through = null;
        List<String> errors = new ArrayList<>();
        List<Holding> funds = holdings.findByDeletedAtIsNullOrderByIdAsc();
        for (Holding h : funds) {
            RefreshResult r = refresh(h, today);
            fetched += r.fetched();
            inserted += r.inserted();
            updated += r.updated();
            // with one fund the message is just the provider's, as it always was
            if (r.error() != null) errors.add(funds.size() == 1 ? r.error() : h.getTicker() + ": " + r.error());
            if (r.latestDate() != null && (through == null || r.latestDate().isBefore(through))) through = r.latestDate();
        }
        return new RefreshResult(fetched, inserted, updated, through, errors.isEmpty() ? null : String.join("; ", errors));
    }

    /**
     * Fetches from the day after the newest cached close (minus a small overlap), or, on first
     * run, from the fund's first buy (or, with none yet, a month back). Safe to call repeatedly.
     */
    private RefreshResult refresh(Holding holding, LocalDate today) {
        String ticker = holding.getTicker();
        LocalDate latest = prices.findFirstByTickerOrderByPriceDateDesc(ticker)
                .map(PriceQuote::getPriceDate).orElse(null);
        LocalDate from = latest == null ? backfillStart(holding, today) : latest.minusDays(OVERLAP_DAYS);
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

    /** The fund's first buy, a few days earlier to include the close just before it; a month back with no buys. */
    private LocalDate backfillStart(Holding holding, LocalDate today) {
        LocalDate start = null;
        for (var c : contributions.findByDeletedAtIsNullOrderByContributedOnDesc()) {
            if (!c.getHolding().getId().equals(holding.getId())) continue;
            if (start == null || c.getContributedOn().isBefore(start)) start = c.getContributedOn();
        }
        if (start == null) start = today.minusMonths(1);
        return start.minusDays(OVERLAP_DAYS);
    }
}
