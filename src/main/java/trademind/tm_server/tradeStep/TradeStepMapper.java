package trademind.tm_server.tradeStep;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import trademind.tm_server.trade.TradeMapper;
import trademind.tm_server.tradeStep.dto.NewTradeStepRequest;
import trademind.tm_server.tradeStep.dto.TradeStepResponse;

@Mapper(componentModel = "spring", uses = TradeMapper.class)
public interface TradeStepMapper {
    @Mapping(source = "trade.id", target = "tradeId")
    TradeStepResponse toResponse(TradeStep tradeStep);

    TradeStep toEntity(NewTradeStepRequest request);
}
