package trademind.tm_server.invest.journal;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.InvestTag;
import trademind.tm_server.invest.JournalEntry;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.journal.JournalDtos.CheckinTemplate;
import trademind.tm_server.invest.journal.JournalDtos.EntryRequest;
import trademind.tm_server.invest.journal.JournalDtos.EntryResponse;
import trademind.tm_server.invest.journal.JournalDtos.PageResponse;
import trademind.tm_server.invest.journal.JournalDtos.TagResponse;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.InvestTagRepository;
import trademind.tm_server.invest.repository.JournalEntryRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class JournalService {

    public static final int MAX_BODY = 20_000;
    public static final int MAX_TAGS = 10;
    public static final int MAX_PAGE_SIZE = 100;

    /** Filters for the timeline; every field is optional. */
    public record Filter(String tag, JournalEntryType type, String query, LocalDate from, LocalDate to) {
    }

    private final JournalEntryRepository entries;
    private final InvestTagRepository tags;
    private final ContributionRepository contributions;
    private final ContributionService contributionService;
    private final InvestSettingsRepository settings;
    private final Clock clock;

    public JournalService(JournalEntryRepository entries, InvestTagRepository tags,
                          ContributionRepository contributions, ContributionService contributionService,
                          InvestSettingsRepository settings, Clock clock) {
        this.entries = entries;
        this.tags = tags;
        this.contributions = contributions;
        this.contributionService = contributionService;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse list(Filter filter, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (filter.from() != null && filter.to() != null && filter.to().isBefore(filter.from())) {
            throw new BadRequestException("'to' cannot be before 'from'");
        }
        Page<JournalEntry> result = entries.findAll(specification(filter),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        // Page first (cheap, correct), then load tags for just those ids in one query.
        List<Long> ids = result.getContent().stream().map(JournalEntry::getId).toList();
        Map<Long, JournalEntry> loaded = entries.findByIdIn(ids).stream()
                .collect(Collectors.toMap(JournalEntry::getId, Function.identity()));
        List<EntryResponse> items = ids.stream().map(loaded::get).map(JournalService::toResponse).toList();
        return new PageResponse(items, page, size, result.getTotalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public EntryResponse get(Long id) {
        return toResponse(find(id));
    }

    public EntryResponse create(EntryRequest request) {
        JournalEntry e = new JournalEntry();
        apply(e, request);
        return toResponse(entries.save(e));
    }

    public EntryResponse update(Long id, EntryRequest request) {
        JournalEntry e = find(id);
        apply(e, request);
        return toResponse(entries.save(e));
    }

    public void delete(Long id) {
        JournalEntry e = find(id);
        e.setDeletedAt(LocalDateTime.now(clock));
        entries.save(e);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tags() {
        return tags.findUsage().stream().map(u -> new TagResponse(u.getName(), u.getCount())).toList();
    }

    /** The month check-in skeleton, with this month's contribution status filled in. */
    @Transactional(readOnly = true)
    public CheckinTemplate checkinTemplate() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        YearMonth month = YearMonth.from(today);

        ContributionStatusCalculator.Result status = contributionService.status();
        var current = status.months().isEmpty() ? null : status.months().get(status.months().size() - 1);
        var state = current == null ? ContributionStatusCalculator.MonthState.PENDING : current.state();

        Long contributionId = contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                .filter(c -> YearMonth.from(c.getContributedOn()).equals(month))
                .max(Comparator.comparing(Contribution::getContributedOn).thenComparing(Contribution::getId))
                .map(Contribution::getId).orElse(null);

        String body = CheckinTemplateBuilder.build(month, state, current == null ? 0 : current.contributions(),
                current == null ? 0 : current.totalCents(), s.getCurrency(), status.currentStreak());
        return new CheckinTemplate(month.toString(), body, contributionId);
    }

    // ---- internals ----------------------------------------------------------------------------

    private void apply(JournalEntry e, EntryRequest r) {
        if (r.body().isBlank()) {
            throw new BadRequestException("Write something first");
        }
        e.setEntryType(r.entryType());
        e.setMood(r.mood());
        e.setBody(r.body().strip());
        e.setContribution(r.contributionId() == null ? null : contributions.findById(r.contributionId())
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new BadRequestException("Contribution " + r.contributionId() + " not found")));
        Set<InvestTag> resolved = new LinkedHashSet<>();
        for (String name : normalizeTags(r.tags())) {
            resolved.add(tags.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                    .orElseGet(() -> tags.save(new InvestTag(name))));
        }
        e.getTags().clear();
        e.getTags().addAll(resolved);
    }

    /** Trim, drop a leading '#', collapse spaces, lower-case, dedupe, keep order. */
    static List<String> normalizeTags(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : raw) {
            String n = t.strip().replaceFirst("^#+", "").strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (n.isEmpty()) continue;
            out.add(n);
        }
        if (out.size() > MAX_TAGS) {
            throw new BadRequestException("At most " + MAX_TAGS + " tags per entry");
        }
        return new ArrayList<>(out);
    }

    private static Specification<JournalEntry> specification(Filter f) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (f.type() != null) {
                p.add(cb.equal(root.get("entryType"), f.type()));
            }
            if (f.tag() != null && !f.tag().isBlank()) {
                var tag = root.join("tags", JoinType.INNER);
                p.add(cb.equal(cb.lower(tag.get("name")), f.tag().strip().replaceFirst("^#+", "").toLowerCase(Locale.ROOT)));
                p.add(cb.isNull(tag.get("deletedAt")));
            }
            if (f.query() != null && !f.query().isBlank()) {
                String escaped = f.query().strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                p.add(cb.like(cb.lower(root.get("body")), "%" + escaped + "%", '\\'));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private JournalEntry find(Long id) {
        return entries.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry " + id + " not found"));
    }

    private static EntryResponse toResponse(JournalEntry e) {
        List<String> tagNames = e.getTags().stream()
                .filter(t -> t.getDeletedAt() == null)
                .map(InvestTag::getName).sorted().toList();
        return new EntryResponse(e.getId(), e.getEntryType(), e.getMood(), e.getBody(), tagNames,
                e.getContribution() == null ? null : e.getContribution().getId(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
