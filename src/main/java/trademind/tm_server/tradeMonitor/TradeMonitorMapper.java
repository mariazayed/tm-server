package trademind.tm_server.tradeMonitor;

import org.mapstruct.Mapper;
import trademind.tm_server.tradeMonitor.dto.NewTradeMonitorRequest;
import trademind.tm_server.tradeMonitor.dto.TradeMonitorResponse;
import trademind.tm_server.tradeMonitor.dto.UpdateTradeMonitorRequest;

@Mapper(componentModel = "spring")
public interface TradeMonitorMapper {
    TradeMonitorResponse toResponse(TradeMonitor tradeMonitor);

    TradeMonitor toEntity(NewTradeMonitorRequest request);

    TradeMonitor toEntity(UpdateTradeMonitorRequest request);
}

