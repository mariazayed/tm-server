package trademind.tm_server.trade;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface TradeMapper {
    TradeResponse toResponse(Trade trade);

    Trade toEntity(NewTradeRequest request);
}