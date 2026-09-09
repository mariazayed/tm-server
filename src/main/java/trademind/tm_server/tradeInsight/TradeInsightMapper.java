package trademind.tm_server.tradeInsight;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import trademind.tm_server.trade.TradeMapper;
import trademind.tm_server.tradeInsight.dto.NewTradeInsightRequest;
import trademind.tm_server.tradeInsight.dto.TradeInsightResponse;

@Mapper(componentModel = "spring", uses = TradeMapper.class)
public interface TradeInsightMapper {
    @Mapping(source = "trade.id", target = "tradeId")
    TradeInsightResponse toResponse(TradeInsight tradeInsight);

    TradeInsight toEntity(NewTradeInsightRequest request);
}
