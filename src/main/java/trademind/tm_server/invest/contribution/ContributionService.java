package trademind.tm_server.invest.contribution;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.dto.ImportResult;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ContributionService {

    private final ContributionRepository contributions;
    private final InvestSettingsRepository settings;
    private final Clock clock;

    public ContributionService(ContributionRepository contributions, InvestSettingsRepository settings,
                               Clock clock) {
        this.contributions = contributions;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InvestSettingsResponse getSettings() {
        return InvestSettingsResponse.from(loadSettings());
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> list() {
        return contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                .map(ContributionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ContributionResponse get(Long id) {
        return ContributionResponse.from(find(id));
    }

    public ContributionResponse create(ContributionRequest raw) {
        ContributionRequest request = ContributionValidator.normalize(raw);
        ContributionValidator.validate(request, today());
        Contribution c = new Contribution();
        apply(c, request);
        return ContributionResponse.from(contributions.save(c));
    }

    public ContributionResponse update(Long id, ContributionRequest raw) {
        ContributionRequest request = ContributionValidator.normalize(raw);
        ContributionValidator.validate(request, today());
        Contribution c = find(id);
        apply(c, request);
        return ContributionResponse.from(contributions.save(c));
    }

    public void delete(Long id) {
        Contribution c = find(id);
        c.setDeletedAt(LocalDateTime.now(clock));
        contributions.save(c);
    }

    @Transactional(readOnly = true)
    public ContributionStatusCalculator.Result status() {
        InvestSettings s = loadSettings();
        List<ContributionStatusCalculator.Entry> entries =
                contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                        .map(c -> new ContributionStatusCalculator.Entry(c.getContributedOn(), c.getAmountCents()))
                        .toList();
        return ContributionStatusCalculator.calculate(s.getStartDate(), s.getBuyDay(), today(), entries);
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        List<Contribution> all = new ArrayList<>(contributions.findByDeletedAtIsNullOrderByContributedOnDesc());
        java.util.Collections.reverse(all); // oldest first reads naturally and re-imports cleanly
        return ContributionCsv.export(all);
    }

    /** All-or-nothing: any invalid row means nothing is saved and every problem is reported. */
    public ImportResult importCsv(String csv) {
        ContributionCsv.ParseResult parsed = ContributionCsv.parse(csv);
        List<ImportResult.RowError> errors = new ArrayList<>(parsed.errors());
        LocalDate today = today();

        Set<String> existing = new HashSet<>();
        for (Contribution c : contributions.findByDeletedAtIsNullOrderByContributedOnDesc()) {
            existing.add(fingerprint(c.getContributedOn(), c.getAmountCents(), c.getShares(), c.getPrice()));
        }

        List<ContributionRequest> toSave = new ArrayList<>();
        int skipped = 0;
        for (ContributionCsv.ParsedRow row : parsed.rows()) {
            ContributionRequest r;
            try {
                r = ContributionValidator.normalize(row.request());
                ContributionValidator.validate(r, today);
            } catch (RuntimeException e) {
                errors.add(new ImportResult.RowError(row.line(), e.getMessage()));
                continue;
            }
            if (existing.add(fingerprint(r.contributedOn(), r.amountCents(), r.shares(), r.price()))) {
                toSave.add(r);
            } else {
                skipped++;
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult(0, 0, errors);
        }
        for (ContributionRequest r : toSave) {
            Contribution c = new Contribution();
            apply(c, r);
            contributions.save(c);
        }
        return new ImportResult(toSave.size(), skipped, List.of());
    }

    private static String fingerprint(LocalDate date, long cents, java.math.BigDecimal shares,
                                      java.math.BigDecimal price) {
        return date + "|" + cents + "|" + shares.stripTrailingZeros().toPlainString()
                + "|" + price.stripTrailingZeros().toPlainString();
    }

    private static void apply(Contribution c, ContributionRequest r) {
        c.setContributedOn(r.contributedOn());
        c.setAmountCents(r.amountCents());
        c.setShares(r.shares());
        c.setPrice(r.price());
        c.setFeeCents(r.feeCents() == null ? 0 : r.feeCents());
        c.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
    }

    private Contribution find(Long id) {
        return contributions.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution " + id + " not found"));
    }

    private InvestSettings loadSettings() {
        return settings.findById(InvestSettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Invest settings not found"));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of(loadSettings().getTimezone())));
    }
}
