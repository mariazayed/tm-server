package trademind.tm_server.reminderNote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.reminderNote.dto.ReminderNoteResponse;
import trademind.tm_server.reminderNote.dto.NewReminderNoteRequest;
import trademind.tm_server.reminderNote.repository.ReminderNoteRepository;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
public class ReminderNoteService {

    @Autowired
    private ReminderNoteRepository repository;

    @Autowired
    private ReminderNoteMapper mapper;

    private final String tableName = "Reminder Notes";

    public List<ReminderNoteResponse> create(List<NewReminderNoteRequest> requests) {
        List<ReminderNote> reminderNoteEntities = requests.stream()
                .map(mapper::toEntity)
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
}