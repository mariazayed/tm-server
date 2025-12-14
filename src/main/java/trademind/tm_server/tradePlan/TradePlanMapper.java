package trademind.tm_server.tradePlan;

import org.mapstruct.Mapper;
import trademind.tm_server.tradePlan.dto.NewTradePlanRequest;
import trademind.tm_server.tradePlan.dto.TradePlanResponse;
import trademind.tm_server.tradePlan.dto.UpdateTradePlanRequest;

@Mapper(componentModel = "spring")
public interface TradePlanMapper {
    TradePlanResponse toResponse(TradePlan tradePlan);

    TradePlan toEntity(NewTradePlanRequest request);

    TradePlan toEntity(UpdateTradePlanRequest request);
}

