package trademind.tm_server.reminderNote;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryRequest;
import trademind.tm_server.reminderNote.dto.ReminderNoteCategoryResponse;

@Mapper(componentModel = "spring")
public interface ReminderNoteCategoryMapper {
    ReminderNoteCategoryResponse toResponse(ReminderNoteCategory category);
    ReminderNoteCategory toEntity(ReminderNoteCategoryRequest request);
    void update(ReminderNoteCategoryRequest request, @MappingTarget ReminderNoteCategory category);
}
