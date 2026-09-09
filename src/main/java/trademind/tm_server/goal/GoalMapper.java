package trademind.tm_server.goal;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import trademind.tm_server.goal.dto.GoalResponse;
import trademind.tm_server.goal.dto.NewGoalRequest;
import trademind.tm_server.goal.dto.UpdateGoalRequest;

@Mapper(componentModel = "spring")
public interface GoalMapper {
    GoalResponse toResponse(Goal goal);

    Goal toEntity(NewGoalRequest request);

    void updateEntity(UpdateGoalRequest dto, @MappingTarget Goal entity);
}
