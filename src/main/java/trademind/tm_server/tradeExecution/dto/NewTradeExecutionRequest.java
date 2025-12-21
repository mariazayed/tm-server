package trademind.tm_server.tradeExecution.dto;

import jakarta.validation.constraints.NotNull;
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
public class NewTradeExecutionRequest {
    /// ********** Market Context **********
    @NotNull(message = "Required")
    private TradeDirectionEnum direction;

    @NotNull(message = "Required")
    private MarketBiasEnum marketBias;

    @NotNull(message = "Required")
    private MarketStateEnum marketState;

    @NotNull(message = "Required")
    private VolatilityEnum volatilityExpectation;

    /// ********** Entry Strategy **********
    @NotNull(message = "Required")
    private List<EntryReasonEnum> entryReasons;

    @NotNull(message = "Required")
    private List<EntryConfirmationEnum> entryConfirmations;

    @NotNull(message = "Required")
    private EntryZoneTypeEnum entryZoneType;

    @NotNull(message = "Required")
    private double entryZoneStartLevel;

    @NotNull(message = "Required")
    private double entryZoneEndLevel;

    @NotNull(message = "Required")
    private double estimatedEntryLevel;

    /// ********** Invalidation and Scaling **********
    @NotNull(message = "Required")
    private double invalidationStartLevel;

    @NotNull(message = "Required")
    private double invalidationEndLevel;

    @NotNull(message = "Required")
    private InvalidationConditionEnum invalidationCondition;

    @NotNull(message = "Required")
    private boolean willPyramid;

    @NotNull(message = "Required")
    private List<ScalingRuleEnum> scalingRules;

    @NotNull(message = "Required")
    private boolean willBoost;

    @NotNull(message = "Required")
    private List<BoostRuleEnum> boostRules;

    /// ********** Risk and Money Management **********
    @NotNull(message = "Required")
    private double lotSize;

    @NotNull(message = "Required")
    private double riskRewardRatio;

    /// ********** Stop Loss Details **********
    @NotNull(message = "Required")
    private double stopLossPrice;

    @NotNull(message = "Required")
    private double stopLossInPips;

    @NotNull(message = "Required")
    private double stopLossInUSD;

    @NotNull(message = "Required")
    private StopLossPlacementReasonEnum stopLossPlacementLogic;

    /// ********** Take Profit Details **********
    @NotNull(message = "Required")
    private double takeProfitPrice;

    @NotNull(message = "Required")
    private double takeProfitInPips;

    @NotNull(message = "Required")
    private double takeProfitInUSD;

    @NotNull(message = "Required")
    private TakeProfitPlacementReasonEnum takeProfitPlacementLogic;

    /// ********** Psychological and Focus Factors **********
    @NotNull(message = "Required")
    private ScoreEnum focusLevel;

    @NotNull(message = "Required")
    private ScoreEnum stressLevel;

    @NotNull(message = "Required")
    private ScoreEnum confidenceLevel;

    /// ********** Trade Quality and Performance **********
    @NotNull(message = "Required")
    private ProbabilityEnum expectedProbabilityOfSuccess;

    /// ********** Additional Details **********
    private String notes;
}
