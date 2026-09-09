package trademind.tm_server.trade;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.strategy.StrategyMapper;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;

@Mapper(componentModel = "spring", uses = {AssetMapper.class, StrategyMapper.class})
public interface TradeMapper {
    TradeResponse toResponse(Trade trade);

    Trade toEntity(NewTradeRequest request);

    // MapStruct helper: allows mapping Long tradeId -> Trade trade (used by
    // TradeStepMapper / TradeInsightMapper via uses = TradeMapper.class)
    default Trade map(Long id) {
        if (id == null) {
            return null;
        }
        Trade trade = new Trade();
        trade.setId(id);
        return trade;
    }
}