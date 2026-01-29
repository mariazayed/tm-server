package trademind.tm_server.reminderNote;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.reminderNote.dto.ReminderNoteResponse;
import trademind.tm_server.reminderNote.dto.NewReminderNoteRequest;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface ReminderNoteMapper {
    ReminderNoteResponse toResponse(ReminderNote reminderNote);

    ReminderNote toEntity(NewReminderNoteRequest request);
}