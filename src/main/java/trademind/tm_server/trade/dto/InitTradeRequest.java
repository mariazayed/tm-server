package trademind.tm_server.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitTradeRequest {
    private DayOfWeekEnum dayOfWeek;
    private Long assetId;
}
