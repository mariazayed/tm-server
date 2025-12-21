package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.tradeEvaluate.TradeEvaluate;
import trademind.tm_server.tradePlan.TradePlan;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADES)
public class Trade extends BaseEntity {
    @Column(nullable = false)
    private DayOfWeekEnum dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_plan_id", referencedColumnName = "id")
    private TradePlan tradePlan;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_evaluation_id", referencedColumnName = "id")
    private TradeEvaluate tradeEvaluation;

    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TradeSteps> tradeSteps;
}