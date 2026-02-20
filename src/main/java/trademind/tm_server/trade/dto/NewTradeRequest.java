package trademind.tm_server.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeRequest {
    @NotNull(message = "Required")
    private Long asset;

    @NotNull(message = "Required")
    private LocalDateTime entryDateTime;

    @NotNull(message = "Required")
    private LocalDateTime exitDateTime;

    @NotNull(message = "Required")
    private double entryPrice;

    @NotNull(message = "Required")
    private double exitPrice;

    @NotNull(message = "Required")
    private double slPrice;

    @NotNull(message = "Required")
    private double tpPrice;

    @NotNull(message = "Required")
    private TradeDirectionEnum direction;

    @NotNull(message = "Required")
    private double lotSize;

    @NotNull(message = "Required")
    private double pips;

    @NotNull(message = "Required")
    private double pnl;

    private Long strategy;

    private String notes;
}
