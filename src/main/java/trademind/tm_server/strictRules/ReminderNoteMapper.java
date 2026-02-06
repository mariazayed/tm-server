package trademind.tm_server.strictRules;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.strictRules.dto.NewStrictRuleRequest;
import trademind.tm_server.strictRules.dto.StrictRuleResponse;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface ReminderNoteMapper {
    StrictRuleResponse toResponse(StrictRule strictRule);

    StrictRule toEntity(NewStrictRuleRequest request);
}