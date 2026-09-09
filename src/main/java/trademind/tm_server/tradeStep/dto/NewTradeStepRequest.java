package trademind.tm_server.tradeStep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.TradeUpdateEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeStepRequest {
    @NotNull(message = "Required")
    private Long trade;

    @NotNull(message = "Required")
    private TradeUpdateEnum type;

    @NotNull(message = "Required")
    private LocalDateTime occurredAt;

    private Double priceAtEvent;

    private String notes;
}
