package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.TradeDirectionEnum;
import trademind.tm_server.strategy.Strategy;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADES)
public class Trade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

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
}