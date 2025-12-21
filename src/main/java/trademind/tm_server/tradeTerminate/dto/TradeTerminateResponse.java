package trademind.tm_server.tradeTerminate.dto;

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
public class TradeTerminateResponse {
    private int id;
    private String notes;
    private LocalDateTime exitedAt;
    private double exitPrice;
    private boolean earlyExit;
    private List<ExitReasonEnum> exitReasons;
}
