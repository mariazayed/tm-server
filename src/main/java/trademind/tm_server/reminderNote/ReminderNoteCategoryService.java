package trademind.tm_server.reminderNote;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryRequest;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryResponse;
import trademind.tm_server.reminderNote.repository.ReminderNoteCategoryRepository;
import trademind.tm_server.reminderNote.repository.ReminderNoteRepository;
import trademind.tm_server.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderNoteCategoryService {
    private final ReminderNoteCategoryRepository categoryRepository;
    private final ReminderNoteRepository noteRepository;
    private final ReminderNoteCategoryMapper mapper;

    public ReminderNoteCategoryResponse create(ReminderNoteCategoryRequest request) {
        request.setName(StringUtils.capitalizeFirst(request.getName().trim()));
        return mapper.toResponse(categoryRepository.save(mapper.toEntity(request)));
    }

    public List<ReminderNoteCategoryResponse> getAll() {
        return categoryRepository.findByDeletedAtIsNullOrderByNameAsc().stream().map(mapper::toResponse).toList();
    }

    public ReminderNoteCategoryResponse update(Long id, ReminderNoteCategoryRequest request) {
        ReminderNoteCategory category = getCategory(id);
        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(StringUtils.capitalizeFirst(request.getName().trim()));
        }
        return mapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        ReminderNoteCategory category = getCategory(id);
        noteRepository.clearCategory(id);
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private ReminderNoteCategory getCategory(Long id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder Note Category", "id", id));
    }
}
