package trademind.tm_server.tradeStep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.TradeUpdateEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeStepResponse {
    private Long id;
    private Long tradeId;
    private TradeUpdateEnum type;
    private LocalDateTime occurredAt;
    private Double priceAtEvent;
    private String notes;
}
