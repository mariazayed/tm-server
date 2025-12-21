package trademind.tm_server.tradePlan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTradePlanRequest {
    /// ********** Market Context **********
    private TradeDirectionEnum direction;
    private MarketBiasEnum marketBias;
    private MarketStateEnum marketState;
    private VolatilityEnum volatilityExpectation;

    /// ********** Entry Strategy **********
    private List<EntryReasonEnum> entryReasons;
    private List<EntryConfirmationEnum> entryConfirmations;
    private EntryZoneTypeEnum entryZoneType;
    private double entryZoneStartLevel;
    private double entryZoneEndLevel;
    private double estimatedEntryLevel;

    /// ********** Invalidation and Scaling **********
    private double invalidationStartLevel;
    private double invalidationEndLevel;
    private InvalidationConditionEnum invalidationCondition;
    private boolean willPyramid;
    private List<ScalingRuleEnum> scalingRules;
    private boolean willBoost;
    private List<BoostRuleEnum> boostRules;

    /// ********** Risk and Money Management **********
    private double lotSize;
    private double riskRewardRatio;

    /// ********** Stop Loss Details **********
    private double stopLossPrice;
    private double stopLossInPips;
    private double stopLossInUSD;
    private StopLossPlacementReasonEnum stopLossPlacementLogic;

    /// ********** Take Profit Details **********
    private double takeProfitPrice;
    private double takeProfitInPips;
    private double takeProfitInUSD;
    private TakeProfitPlacementReasonEnum takeProfitPlacementLogic;

    /// ********** Psychological and Focus Factors **********
    private ScoreEnum focusLevel;
    private ScoreEnum stressLevel;
    private ScoreEnum confidenceLevel;

    /// ********** Trade Quality and Performance **********
    private ProbabilityEnum expectedProbabilityOfSuccess;

    /// ********** Additional Details **********
    private String notes;
}
