package trademind.tm_server.invest.learning;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.InvestTag;
import trademind.tm_server.invest.LearningNote;
import trademind.tm_server.invest.LearningSource;
import trademind.tm_server.invest.TagService;
import trademind.tm_server.invest.learning.LearningDtos.NotePage;
import trademind.tm_server.invest.learning.LearningDtos.NoteRequest;
import trademind.tm_server.invest.learning.LearningDtos.NoteResponse;
import trademind.tm_server.invest.learning.LearningDtos.SourceRequest;
import trademind.tm_server.invest.learning.LearningDtos.SourceResponse;
import trademind.tm_server.invest.journal.JournalDtos.TagResponse;
import trademind.tm_server.invest.repository.InvestTagRepository;
import trademind.tm_server.invest.repository.LearningNoteRepository;
import trademind.tm_server.invest.repository.LearningSourceRepository;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reading list (sources) and learning notes. */
@Service
@Transactional
public class LearningService {

    public static final int MAX_PAGE_SIZE = 100;

    public record NoteFilter(Long sourceId, String tag, String query) {
    }

    private final LearningSourceRepository sources;
    private final LearningNoteRepository notes;
    private final InvestTagRepository tags;
    private final TagService tagService;
    private final Clock clock;

    public LearningService(LearningSourceRepository sources, LearningNoteRepository notes,
                           InvestTagRepository tags, TagService tagService, Clock clock) {
        this.sources = sources;
        this.notes = notes;
        this.tags = tags;
        this.tagService = tagService;
        this.clock = clock;
    }

    // ---- sources ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SourceResponse> listSources(SourceStatus status) {
        return sources.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .map(LearningService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SourceResponse getSource(Long id) {
        return toResponse(findSource(id));
    }

    public SourceResponse createSource(SourceRequest r) {
        LearningSource s = new LearningSource();
        apply(s, r);
        return toResponse(sources.save(s));
    }

    public SourceResponse updateSource(Long id, SourceRequest r) {
        LearningSource s = findSource(id);
        apply(s, r);
        return toResponse(sources.save(s));
    }

    public void deleteSource(Long id) {
        LearningSource s = findSource(id);
        s.setDeletedAt(LocalDateTime.now(clock));
        sources.save(s);
    }

    // ---- notes --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public NotePage listNotes(NoteFilter filter, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<LearningNote> result = notes.findAll(specification(filter), PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<Long> ids = result.getContent().stream().map(LearningNote::getId).toList();
        Map<Long, LearningNote> loaded = notes.findByIdIn(ids).stream()
                .collect(Collectors.toMap(LearningNote::getId, Function.identity()));
        return new NotePage(ids.stream().map(loaded::get).map(LearningService::toResponse).toList(),
                page, size, result.getTotalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(Long id) {
        return toResponse(findNote(id));
    }

    public NoteResponse createNote(NoteRequest r) {
        LearningNote n = new LearningNote();
        apply(n, r);
        return toResponse(notes.save(n));
    }

    public NoteResponse updateNote(Long id, NoteRequest r) {
        LearningNote n = findNote(id);
        apply(n, r);
        return toResponse(notes.save(n));
    }

    public void deleteNote(Long id) {
        LearningNote n = findNote(id);
        n.setDeletedAt(LocalDateTime.now(clock));
        notes.save(n);
    }

    /** Tags with how many live learning notes use them (for filtering and autocomplete). */
    @Transactional(readOnly = true)
    public List<TagResponse> noteTags() {
        return tags.findNoteUsage().stream().map(u -> new TagResponse(u.getName(), u.getCount())).toList();
    }

    // ---- internals ----------------------------------------------------------------------------

    private void apply(LearningSource s, SourceRequest r) {
        if (r.title().isBlank()) throw new BadRequestException("Title is required");
        s.setTitle(r.title().strip());
        s.setSourceType(r.sourceType());
        s.setUrl(validUrl(r.url()));
        s.setStatus(r.status() == null ? SourceStatus.TO_DO : r.status());
        s.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().strip());
    }

    /** Only http(s) links are accepted, so a stored link can never be a javascript: or file: URL. */
    static String validUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String url = raw.strip();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
                throw new BadRequestException("URL must be a valid http or https link");
            }
        } catch (java.net.URISyntaxException e) {
            throw new BadRequestException("URL must be a valid http or https link");
        }
        return url;
    }

    private void apply(LearningNote n, NoteRequest r) {
        if (r.title().isBlank() || r.body().isBlank()) {
            throw new BadRequestException("Title and body are required");
        }
        n.setTitle(r.title().strip());
        n.setBody(r.body().strip());
        n.setSource(r.sourceId() == null ? null : findSourceOrBadRequest(r.sourceId()));
        var resolved = tagService.resolve(r.tags());
        n.getTags().clear();
        n.getTags().addAll(resolved);
    }

    private LearningSource findSourceOrBadRequest(Long id) {
        return sources.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BadRequestException("Source " + id + " not found"));
    }

    private LearningSource findSource(Long id) {
        return sources.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source " + id + " not found"));
    }

    private LearningNote findNote(Long id) {
        return notes.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning note " + id + " not found"));
    }

    private static Specification<LearningNote> specification(NoteFilter f) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (f.sourceId() != null) {
                p.add(cb.equal(root.get("source").get("id"), f.sourceId()));
            }
            if (f.tag() != null && !f.tag().isBlank()) {
                var tag = root.join("tags", JoinType.INNER);
                p.add(cb.equal(cb.lower(tag.get("name")), f.tag().strip().replaceFirst("^#+", "").toLowerCase(Locale.ROOT)));
                p.add(cb.isNull(tag.get("deletedAt")));
            }
            if (f.query() != null && !f.query().isBlank()) {
                String like = "%" + f.query().strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("body")), like, '\\')));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static SourceResponse toResponse(LearningSource s) {
        return new SourceResponse(s.getId(), s.getTitle(), s.getSourceType(), s.getUrl(), s.getStatus(),
                s.getNotes(), s.getCreatedAt());
    }

    private static NoteResponse toResponse(LearningNote n) {
        LearningSource src = n.getSource();
        boolean liveSource = src != null && src.getDeletedAt() == null;
        return new NoteResponse(n.getId(), n.getTitle(), n.getBody(),
                liveSource ? src.getId() : null, liveSource ? src.getTitle() : null,
                n.getTags().stream().filter(t -> t.getDeletedAt() == null).map(InvestTag::getName).sorted().toList(),
                n.getCreatedAt(), n.getUpdatedAt());
    }
}
