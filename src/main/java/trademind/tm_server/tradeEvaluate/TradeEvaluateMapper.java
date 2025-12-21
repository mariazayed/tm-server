package trademind.tm_server.tradeEvaluate;

import org.mapstruct.Mapper;
import trademind.tm_server.tradeEvaluate.dto.NewTradeEvaluateRequest;
import trademind.tm_server.tradeEvaluate.dto.TradeEvaluateResponse;
import trademind.tm_server.tradeEvaluate.dto.UpdateTradeEvaluateRequest;

@Mapper(componentModel = "spring")
public interface TradeEvaluateMapper {
    TradeEvaluateResponse toResponse(TradeEvaluate tradeEvaluate);

    TradeEvaluate toEntity(NewTradeEvaluateRequest request);

    TradeEvaluate toEntity(UpdateTradeEvaluateRequest request);
}

