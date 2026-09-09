package trademind.tm_server.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.enums.*;
import trademind.tm_server.strategy.dto.StrategyResponse;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {
    private Long id;
    private AssetResponse asset;

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

    private StrategyResponse strategy;
    private String notes;

    // --- Source & traceability ---
    private String externalId;
    private TradeSourceEnum source;
    private LocalDateTime reviewedAt;

    // --- Setup context ---
    private SessionEnum session;
    private DayOfWeekEnum dayOfWeek;
    private VolatilityEnum volatility;
    private TradeTypeEnum tradeType;
    private MarketStateEnum marketState;
    private MarketBiasEnum marketBias;
    private EntryReasonEnum entryReason;
    private EntryConfirmationEnum entryConfirmation;
    private EntryZoneTypeEnum entryZoneType;
    private ProbabilityEnum probability;

    // --- Risk & sizing ---
    private Double riskPercent;
    private Double accountBalanceAtEntry;
    private Double plannedRiskReward;
    private Double rMultiple;
    private ScalingRuleEnum scalingRule;

    // --- Trade management & outcome ---
    private Double mae;
    private Double mfe;
    private ExitReasonEnum exitReason;
    private InvalidationConditionEnum invalidationCondition;
    private StopLossPlacementReasonEnum slPlacementReason;
    private TakeProfitPlacementReasonEnum tpPlacementReason;

    // --- Discipline & psychology ---
    private Boolean followedPlan;
    private ScoreEnum disciplineScore;
    private Set<MistakeTagEnum> mistakeTags;

    // --- AI-assisted structure detection (Phase 5) ---
    // JSON-encoded list of structure events (swing breaks, order blocks,
    // FVGs, liquidity sweeps) detected by the MT5 importer's deterministic,
    // rule-based structure_detection.py -- feeds tm-server's
    // AiSuggestionService as context for the fields it fills in (see that
    // class for which fields those are). Plain text, not a real relational
    // shape, on purpose: this is read by a prompt builder, not queried.
    private String detectedStructures;
}
