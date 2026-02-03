package trademind.tm_server.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.asset.repository.AssetRepository;
import trademind.tm_server.day.Day;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {
    private AssetResponse asset;

    private LocalDateTime entryDateTime;
    private LocalDateTime exitDateTime;

    private double entryPrice;
    private double exitPrice;

    private double slPrice;
    private double tpPrice;

    private TradeDirectionEnum direction;
    private double lotSize;
    private double pips;
    private double pnl;

    private String notes;
}
