package trademind.tm_server.tradeTerminate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.ExitReasonEnum;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeTerminateRequest {
    @NotNull(message = "Required")
    private LocalDateTime exitedAt;

    @NotNull(message = "Required")
    private double exitPrice;

    @NotNull(message = "Required")
    private boolean earlyExit;

    @NotNull(message = "Required")
    private List<ExitReasonEnum> exitReasons;

    private String notes;
}
