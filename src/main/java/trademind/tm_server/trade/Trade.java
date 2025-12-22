package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.day.Day;
import trademind.tm_server.tradeEvaluate.TradeEvaluate;
import trademind.tm_server.tradePlan.TradePlan;

import java.util.List;


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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private Day day;

    @OneToMany(mappedBy = "trades", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeSteps> tradeSteps;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_plan_id", referencedColumnName = "id")
    private TradePlan tradePlan;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_evaluation_id", referencedColumnName = "id")
    private TradeEvaluate tradeEvaluation;
}