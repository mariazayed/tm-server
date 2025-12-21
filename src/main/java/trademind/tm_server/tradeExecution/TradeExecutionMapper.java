package trademind.tm_server.tradeExecution;

import org.mapstruct.Mapper;
import trademind.tm_server.tradeExecution.dto.NewTradeExecutionRequest;
import trademind.tm_server.tradeExecution.dto.TradeExecutionResponse;
import trademind.tm_server.tradeExecution.dto.UpdateTradeExecutionRequest;

@Mapper(componentModel = "spring")
public interface TradeExecutionMapper {
    TradeExecutionResponse toResponse(TradeExecution tradeExecution);

    TradeExecution toEntity(NewTradeExecutionRequest request);

    TradeExecution toEntity(UpdateTradeExecutionRequest request);
}

