package trademind.tm_server.tradeTerminate;

import org.mapstruct.Mapper;
import trademind.tm_server.tradeTerminate.dto.NewTradeTerminateRequest;
import trademind.tm_server.tradeTerminate.dto.TradeTerminateResponse;
import trademind.tm_server.tradeTerminate.dto.UpdateTradeTerminateRequest;

@Mapper(componentModel = "spring")
public interface TradeTerminateMapper {
    TradeTerminateResponse toResponse(TradeTerminate tradeTerminate);

    TradeTerminate toEntity(NewTradeTerminateRequest request);

    TradeTerminate toEntity(UpdateTradeTerminateRequest request);
}

