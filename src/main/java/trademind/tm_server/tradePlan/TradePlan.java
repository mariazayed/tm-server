package trademind.tm_server.tradePlan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;
import trademind.tm_server.trade.Trade;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_PLANS)
public class TradePlan extends BaseEntity {
    /// ********** Market Context **********
    @Column(nullable = false)
    private TradeDirectionEnum direction;

    @Column(nullable = false)
    private MarketBiasEnum marketBias;

    @Column(nullable = false)
    private MarketStateEnum marketState;

    @Column(nullable = false)
    private VolatilityEnum volatilityExpectation;

    /// ********** Entry Strategy **********
    @Column(nullable = false)
    private List<EntryReasonEnum> entryReasons;

    @Column(nullable = false)
    private List<EntryConfirmationEnum> entryConfirmations;

    @Column(nullable = false)
    private EntryZoneTypeEnum entryZoneType;

    @Column(nullable = false)
    private double entryZoneStartLevel;

    @Column(nullable = false)
    private double entryZoneEndLevel;

    @Column(nullable = false)
    private double estimatedEntryLevel;

    /// ********** Invalidation and Scaling **********
    @Column(nullable = false)
    private double invalidationStartLevel;

    @Column(nullable = false)
    private double invalidationEndLevel;

    @Column(nullable = false)
    private InvalidationConditionEnum invalidationCondition;

    @Column(nullable = false)
    private boolean willPyramid;

    @Column(nullable = false)
    private List<ScalingRuleEnum> scalingRules;

    @Column(nullable = false)
    private boolean willBoost;

    @Column(nullable = false)
    private List<BoostRuleEnum> boostRules;

    /// ********** Risk and Money Management **********
    @Column(nullable = false)
    private double lotSize;

    @Column(nullable = false)
    private double riskRewardRatio;

    /// ********** Stop Loss Details **********
    @Column(nullable = false)
    private double stopLossPrice;

    @Column(nullable = false)
    private double stopLossInPips;

    @Column(nullable = false)
    private double stopLossInUSD;

    @Column(nullable = false)
    private StopLossPlacementReasonEnum stopLossPlacementLogic;

    /// ********** Take Profit Details **********
    @Column(nullable = false)
    private double takeProfitPrice;

    @Column(nullable = false)
    private double takeProfitInPips;

    @Column(nullable = false)
    private double takeProfitInUSD;

    @Column(nullable = false)
    private TakeProfitPlacementReasonEnum takeProfitPlacementLogic;

    /// ********** Psychological and Focus Factors **********
    @Column(nullable = false)
    private ScoreEnum focusLevel;

    @Column(nullable = false)
    private ScoreEnum stressLevel;

    @Column(nullable = false)
    private ScoreEnum confidenceLevel;

    /// ********** Trade Quality and Performance **********
    @Column(nullable = false)
    private ProbabilityEnum expectedProbabilityOfSuccess;

    @OneToOne(mappedBy = "tradePlan")
    private Trade trade;
}