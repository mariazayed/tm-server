package trademind.tm_server.invest.holding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.SettingsChange;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.HoldingRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.SettingsChangeRepository;

import java.util.List;
import java.util.Locale;

/**
 * The funds you hold. {@code invest_settings.ticker} follows the first one, so code that still reads the
 * settings ticker keeps working; a change of that ticker is written to the plan history like before.
 */
@Service
@Transactional
public class HoldingService {

    private final HoldingRepository holdings;
    private final ContributionRepository contributions;
    private final InvestSettingsRepository settings;
    private final SettingsChangeRepository changes;
    private final boolean multiple;

    public HoldingService(HoldingRepository holdings, ContributionRepository contributions,
                          InvestSettingsRepository settings, SettingsChangeRepository changes,
                          @Value("${invest.holdings.multiple:true}") boolean multiple) {
        this.holdings = holdings;
        this.contributions = contributions;
        this.settings = settings;
        this.changes = changes;
        this.multiple = multiple;
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> list() {
        return holdings.findByDeletedAtIsNullOrderByIdAsc().stream().map(HoldingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public HoldingResponse get(Long id) {
        return HoldingResponse.from(find(id));
    }

    public HoldingResponse create(HoldingRequest r) {
        if (!multiple && !holdings.findByDeletedAtIsNullOrderByIdAsc().isEmpty()) {
            throw new BadRequestException("Tracking more than one fund is not switched on yet");
        }
        String ticker = ticker(r);
        ensureTickerFree(ticker, null);
        Holding h = new Holding();
        apply(h, r, ticker);
        Holding saved = holdings.save(h);
        syncSettingsTicker();
        return HoldingResponse.from(saved);
    }

    public HoldingResponse update(Long id, HoldingRequest r) {
        Holding h = find(id);
        String ticker = ticker(r);
        ensureTickerFree(ticker, id);
        apply(h, r, ticker);
        Holding saved = holdings.save(h);
        syncSettingsTicker();
        return HoldingResponse.from(saved);
    }

    public void delete(Long id) {
        Holding h = find(id);
        if (contributions.existsByHoldingIdAndDeletedAtIsNull(id)) {
            throw new BadRequestException("This fund has buys recorded. Delete them or move them to another fund first");
        }
        if (holdings.findByDeletedAtIsNullOrderByIdAsc().size() <= 1) {
            throw new BadRequestException("At least one fund is needed");
        }
        h.setDeletedAt(java.time.LocalDateTime.now());
        holdings.save(h);
        syncSettingsTicker();
    }

    /**
     * Clients from before there were funds change the plan's ticker. With exactly one fund that means "the fund
     * is now this ticker", so it is renamed; with several the ticker is derived and the request value is ignored.
     */
    public void applyLegacyTicker(String requested) {
        if (requested == null || requested.isBlank()) return;
        List<Holding> live = holdings.findByDeletedAtIsNullOrderByIdAsc();
        if (live.size() != 1) return;
        String ticker = requested.strip().toUpperCase(Locale.ROOT);
        Holding only = live.get(0);
        if (only.getTicker().equalsIgnoreCase(ticker)) return;
        ensureTickerFree(ticker, only.getId());
        only.setTicker(ticker);
        holdings.save(only);
        syncSettingsTicker();
    }

    /** Keeps the settings ticker equal to the first live fund, writing the plan history when it changes. */
    void syncSettingsTicker() {
        List<Holding> live = holdings.findByDeletedAtIsNullOrderByIdAsc();
        if (live.isEmpty()) return;
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        String primary = live.get(0).getTicker();
        if (!primary.equals(s.getTicker())) {
            changes.save(new SettingsChange(SettingsChange.TICKER, s.getTicker(), primary));
            s.setTicker(primary);
            settings.save(s);
        }
    }

    private Holding find(Long id) {
        return holdings.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund " + id + " not found"));
    }

    private static String ticker(HoldingRequest r) {
        return r.ticker().strip().toUpperCase(Locale.ROOT);
    }

    private void ensureTickerFree(String ticker, Long exceptId) {
        holdings.findByTickerIgnoreCaseAndDeletedAtIsNull(ticker).ifPresent(other -> {
            if (!other.getId().equals(exceptId)) throw new BadRequestException("You already have a fund with ticker " + ticker);
        });
    }

    private static void apply(Holding h, HoldingRequest r, String ticker) {
        h.setTicker(ticker);
        h.setName(r.name() == null || r.name().isBlank() ? null : r.name().strip());
        h.setAssetClass(r.assetClass());
        h.setRegion(r.region().strip());
        h.setTargetPct(r.targetPct());
        h.setExpenseRatio(r.expenseRatio());
        h.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().strip());
    }
}
