package trademind.tm_server.tradeMonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTradeMonitorRequest {
    private String marketBehaviorAfterEntry;
    private boolean anyPlanDeviation;
    private String updates;
    private String notes;
}
