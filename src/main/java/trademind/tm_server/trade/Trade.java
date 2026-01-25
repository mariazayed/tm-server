package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADES)
public class Trade extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    private LocalDateTime entryDateTime;
    private LocalDateTime exitDateTime;

    private double entryPrice;
    private double exitPrice;

    private TradeDirectionEnum direction;
    private double lotSize;
    private double pips;
    private double pnl;
}