package trademind.tm_server.strategy;

import org.mapstruct.Mapper;
import trademind.tm_server.strategy.dto.NewStrategyRequest;
import trademind.tm_server.strategy.dto.StrategyResponse;

@Mapper(componentModel = "spring")
public interface StrategyMapper {
    StrategyResponse toResponse(Strategy strategy);

    Strategy toEntity(NewStrategyRequest request);

    // MapStruct helper: allows mapping Long strategyId -> Strategy strategy
    default Strategy map(Long id) {
        if (id == null) {
            return null;
        }
        Strategy strategy = new Strategy();
        strategy.setId(id);
        return strategy;
    }
}