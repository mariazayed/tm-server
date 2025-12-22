package trademind.tm_server.trade;

import org.mapstruct.Mapper;
import trademind.tm_server.trade.dto.InitTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.dto.UpdateTradeRequest;

@Mapper(componentModel = "spring")
public interface TradeMapper {
    TradeResponse toResponse(Trade trade);

    Trade toEntity(InitTradeRequest request);

    Trade toEntity(UpdateTradeRequest request);
}

