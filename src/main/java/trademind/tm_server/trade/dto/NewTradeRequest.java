package trademind.tm_server.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeRequest {
    @NotNull(message = "Required")
    private Long asset;

    @NotNull(message = "Required")
    private LocalDateTime entryDateTime;

    @NotNull(message = "Required")
    private LocalDateTime exitDateTime;

    @NotNull(message = "Required")
    private double entryPrice;

    @NotNull(message = "Required")
    private double exitPrice;

    @NotNull(message = "Required")
    private double slPrice;

    @NotNull(message = "Required")
    private double tpPrice;

    @NotNull(message = "Required")
    private TradeDirectionEnum direction;

    @NotNull(message = "Required")
    private double lotSize;

    @NotNull(message = "Required")
    private double pips;

    @NotNull(message = "Required")
    private double pnl;

    private Long strategy;

    private String notes;

    // --- Source & traceability (all optional -- manual entries from the app
    // won't set these; the MT5 importer will) ---
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
