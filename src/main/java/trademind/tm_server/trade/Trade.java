package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;
import trademind.tm_server.strategy.Strategy;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADES)
public class Trade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    private LocalDateTime entryDateTime;
    private LocalDateTime exitDateTime;

    private double entryPrice;
    private double exitPrice;

    private double slPrice;
    private double tpPrice;

    @Enumerated(EnumType.STRING)
    private TradeDirectionEnum direction;
    private double lotSize;
    private double pips;
    private double pnl;

    // --- Source & traceability ---
    private String externalId;

    @Enumerated(EnumType.STRING)
    private TradeSourceEnum source;

    private LocalDateTime reviewedAt;

    // --- Setup context ---
    @Enumerated(EnumType.STRING)
    private SessionEnum session;

    @Enumerated(EnumType.STRING)
    private DayOfWeekEnum dayOfWeek;

    @Enumerated(EnumType.STRING)
    private VolatilityEnum volatility;

    @Enumerated(EnumType.STRING)
    private TradeTypeEnum tradeType;

    @Enumerated(EnumType.STRING)
    private MarketStateEnum marketState;

    @Enumerated(EnumType.STRING)
    private MarketBiasEnum marketBias;

    @Enumerated(EnumType.STRING)
    private EntryReasonEnum entryReason;

    @Enumerated(EnumType.STRING)
    private EntryConfirmationEnum entryConfirmation;

    @Enumerated(EnumType.STRING)
    private EntryZoneTypeEnum entryZoneType;

    @Enumerated(EnumType.STRING)
    private ProbabilityEnum probability;

    // --- Risk & sizing ---
    private Double riskPercent;
    private Double accountBalanceAtEntry;
    private Double plannedRiskReward;
    private Double rMultiple;

    @Enumerated(EnumType.STRING)
    private ScalingRuleEnum scalingRule;

    // --- Trade management & outcome ---
    private Double mae;
    private Double mfe;

    @Enumerated(EnumType.STRING)
    private ExitReasonEnum exitReason;

    @Enumerated(EnumType.STRING)
    private InvalidationConditionEnum invalidationCondition;

    @Enumerated(EnumType.STRING)
    private StopLossPlacementReasonEnum slPlacementReason;

    @Enumerated(EnumType.STRING)
    private TakeProfitPlacementReasonEnum tpPlacementReason;

    // --- Discipline & psychology ---
    private Boolean followedPlan;

    @Enumerated(EnumType.STRING)
    private ScoreEnum disciplineScore;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "trade_mistake_tags", joinColumns = @JoinColumn(name = "trade_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_tag")
    private Set<MistakeTagEnum> mistakeTags;

    // --- AI-assisted structure detection (Phase 5) ---
    // JSON-encoded list of structure events (swing breaks, order blocks,
    // FVGs, liquidity sweeps) detected by the MT5 importer's deterministic,
    // rule-based structure_detection.py -- feeds tm-server's
    // AiSuggestionService as context for the fields it fills in. Plain text,
    // not a real relational shape, on purpose: this is read by a prompt
    // builder, not queried.
    @Column(columnDefinition = "text")
    private String detectedStructures;
}