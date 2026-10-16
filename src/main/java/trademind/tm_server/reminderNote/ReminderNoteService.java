package trademind.tm_server.reminderNote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.reminderNote.dto.ReminderNoteResponse;
import trademind.tm_server.reminderNote.dto.NewReminderNoteRequest;
import trademind.tm_server.reminderNote.dto.ReminderNoteSearchRequest;
import trademind.tm_server.reminderNote.repository.ReminderNoteRepository;
import trademind.tm_server.reminderNote.repository.ReminderNoteSearchRepository;
import trademind.tm_server.reminderNote.repository.ReminderNoteCategoryRepository;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReminderNoteService {

    @Autowired
    private ReminderNoteRepository repository;

    @Autowired
    private ReminderNoteSearchRepository searchRepository;

    @Autowired
    private ReminderNoteMapper mapper;

    @Autowired
    private ReminderNoteCategoryRepository categoryRepository;

    private final String tableName = "Reminder Notes";

    public List<ReminderNoteResponse> create(List<NewReminderNoteRequest> requests) {
        List<ReminderNote> reminderNoteEntities = requests.stream()
                .map(request -> {
                    ReminderNote note = mapper.toEntity(request);
                    if (request.getCategoryId() != null) {
                        note.setCategory(categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Reminder Note Category", "id", request.getCategoryId())));
                    }
                    return note;
                })
                .toList();

        List<ReminderNote> savedReminderNotes = repository.saveAll(reminderNoteEntities);

        return savedReminderNotes.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<ReminderNoteResponse> getAll() {
        List<ReminderNote> reminderNotePlanList = repository.findByDeletedAtIsNull();

        return reminderNotePlanList.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ReminderNoteResponse getOneById(Long id) {
        ReminderNote reminderNotePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(reminderNotePlan);
    }

        public void delete(Long id) {
        ReminderNote object = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        object.setDeletedAt(LocalDateTime.now());
        repository.save(object);
    }

    // Real, server-side pagination, added mainly for consistency with the
    // other list screens (reminder-note counts are typically small).
    public SearchResponse<ReminderNoteResponse> search(ReminderNoteSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<ReminderNote> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}
