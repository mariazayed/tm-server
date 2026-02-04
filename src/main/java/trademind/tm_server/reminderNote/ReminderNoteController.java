package trademind.tm_server.reminderNote;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.reminderNote.dto.ReminderNoteResponse;
import trademind.tm_server.reminderNote.dto.NewReminderNoteRequest;
import trademind.tm_server.constants.AppConstants;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_REMINDER_NOTES)
public class ReminderNoteController {
    @Autowired
    private ReminderNoteService service;

    @PostMapping
    public List<ReminderNoteResponse> create(@RequestBody List<NewReminderNoteRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<ReminderNoteResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public ReminderNoteResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}