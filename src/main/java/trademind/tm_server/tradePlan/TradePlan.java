package trademind.tm_server.tradePlan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_PLANS)
public class TradePlan extends BaseEntity {

    @Column(nullable = false)
    private TradeDirectionEnum direction;

    @Column(nullable = false)
    private MarketBiasEnum marketBias;

    @Column(nullable = false)
    private MarketStateEnum marketState;

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
    private ProbabilityEnum expectedProbabilityOfSuccess;

    @Column(nullable = false)
    private boolean willBoost;

    @Column(nullable = false)
    private List<BoostRuleEnum> boostRules;

    @Column(nullable = false)
    private VolatilityEnum volatilityExpectation;

    @Column(nullable = false)
    private ScoreEnum focusLevel;

    @Column(nullable = false)
    private ScoreEnum stressLevel;

    @Column(nullable = false)
    private ScoreEnum confidenceLevel;

    @Column(nullable = false)
    private double stopLossPrice;

    @Column(nullable = false)
    private double stopLossInPips;

    @Column(nullable = false)
    private double stopLossInUSD;

    @Column(nullable = false)
    private StopLossPlacementReasonEnum stopLossPlacementLogic;

    @Column(nullable = false)
    private double takeProfitPrice;

    @Column(nullable = false)
    private double takeProfitInPips;

    @Column(nullable = false)
    private double takeProfitInUSD;

    @Column(nullable = false)
    private TakeProfitPlacementReasonEnum takeProfitPlacementLogic;

    @Column(nullable = false)
    private double riskRewardRatio;

    @Column(nullable = false)
    private double lotSize;
}