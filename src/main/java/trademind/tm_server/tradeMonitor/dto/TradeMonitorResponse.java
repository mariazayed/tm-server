package trademind.tm_server.tradeMonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeMonitorResponse {
    private int id;
    private String notes;
    private String marketBehaviorAfterEntry;
    private boolean anyPlanDeviation;
    private String updates;
}
