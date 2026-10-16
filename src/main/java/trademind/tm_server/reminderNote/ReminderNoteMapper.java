package trademind.tm_server.reminderNote;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.reminderNote.dto.ReminderNoteResponse;
import trademind.tm_server.reminderNote.dto.NewReminderNoteRequest;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface ReminderNoteMapper {
    @org.mapstruct.Mapping(target = "categoryId", source = "category.id")
    @org.mapstruct.Mapping(target = "categoryName", source = "category.name")
    ReminderNoteResponse toResponse(ReminderNote reminderNote);

    @org.mapstruct.Mapping(target = "category", ignore = true)
    ReminderNote toEntity(NewReminderNoteRequest request);
}
