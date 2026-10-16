package trademind.tm_server.reminderNote;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryRequest;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryResponse;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.URL_REMINDER_NOTE_CATEGORIES)
public class ReminderNoteCategoryController {
    private final ReminderNoteCategoryService service;

    @PostMapping
    public ReminderNoteCategoryResponse create(@RequestBody ReminderNoteCategoryRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ReminderNoteCategoryResponse> getAll() {
        return service.getAll();
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public ReminderNoteCategoryResponse update(@PathVariable Long id, @RequestBody ReminderNoteCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
