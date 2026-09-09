package trademind.tm_server.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.*;

import java.util.Set;

// Partial update, same null-means-unchanged convention as UpdateAssetRequest.
// Only carries the fields a person actually confirms/fills during review --
// AI-suggested and manual tiers -- not the automatic/computed ones.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeReviewRequest {
    private EntryReasonEnum entryReason;
    private EntryConfirmationEnum entryConfirmation;
    private EntryZoneTypeEnum entryZoneType;
    private MarketBiasEnum marketBias;
    private MarketStateEnum marketState;
    private InvalidationConditionEnum invalidationCondition;
    private StopLossPlacementReasonEnum slPlacementReason;
    private TakeProfitPlacementReasonEnum tpPlacementReason;
    private ExitReasonEnum exitReason;
    private ProbabilityEnum probability;
    private Boolean followedPlan;
    private ScoreEnum disciplineScore;
    private Set<MistakeTagEnum> mistakeTags;
}
