package trademind.tm_server.strategy;

import org.mapstruct.Mapper;
import trademind.tm_server.strategy.dto.NewStrategyRequest;
import trademind.tm_server.strategy.dto.StrategyResponse;

@Mapper(componentModel = "spring")
public interface StrategyMapper {
    StrategyResponse toResponse(Strategy strategy);

    Strategy toEntity(NewStrategyRequest request);
}