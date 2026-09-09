package trademind.tm_server.staticData.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.staticData.dataTypes.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticDataResponse {
    private List<TradeDirectionDataType> tradeDirections;
    private List<MarketBiasDataType> marketBiases;
    private List<MarketStateDataType> marketStates;
    private List<EntryReasonDataType> entryReasons;
    private List<EntryConfirmationDataType> entryConfirmations;
    private List<EntryZoneTypeDataType> entryZoneTypes;
    private List<InvalidationConditionDataType> invalidationConditions;
    private List<ProbabilityDataType> probabilities;
    private List<VolatilityDataType> volatilities;
    private List<ScalingRuleDataType> scalingRules;
    private List<ScoreDataType> scoreList;
    private List<MistakeTagDataType> mistakeTags;
    private List<StopLossPlacementReasonDataType> stopLossPlacementReasons;
    private List<TakeProfitPlacementReasonDataType> takeProfitPlacementReasons;
    private List<EntryTypeDataType> entryTypes;
    private List<SessionDataType> sessions;
    private List<StrategyTagDataType> strategyTags;
    private List<TradeUpdateDataType> tradeUpdates;
    private List<ExitReasonDataType> exitReasons;
}
