package trademind.tm_server.decision;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import trademind.tm_server.decision.dto.DecisionResponse;
import trademind.tm_server.decision.dto.NewDecisionRequest;
import trademind.tm_server.decision.dto.UpdateDecisionRequest;

@Mapper(componentModel = "spring")
public interface DecisionMapper {
    DecisionResponse toResponse(Decision decision);

    Decision toEntity(NewDecisionRequest request);

    void updateEntity(UpdateDecisionRequest dto, @MappingTarget Decision entity);
}
