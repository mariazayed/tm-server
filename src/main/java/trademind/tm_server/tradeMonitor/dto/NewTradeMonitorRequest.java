package trademind.tm_server.tradeMonitor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeMonitorRequest {
    @NotNull(message = "Required")
    private String marketBehaviorAfterEntry;

    @NotNull(message = "Required")
    private boolean anyPlanDeviation;

    @NotNull(message = "Required")
    private String updates;

    private String notes;
}
