package trademind.tm_server.constants;

import trademind.tm_server.enums.*;
import trademind.tm_server.staticDataTypes.*;

import java.util.List;

public class StaticData {
    public static final List<TradeDirectionDataType> TRADE_DIRECTIONS = List.of(
            new TradeDirectionDataType(
                    TradeDirectionEnum.LONG,
                    1,
                    "Buy",
                    "شراء",
                    ""
            ),
            new TradeDirectionDataType(
                    TradeDirectionEnum.SHORT,
                    1,
                    "Sell",
                    "بيع",
                    ""
            )
    );

    public static final List<MarketBiasDataType> MARKET_BIASES = List.of(
            new MarketBiasDataType(
                    MarketBiasEnum.BULLISH,
                    1,
                    "Bullish",
                    "",
                    ""
            ),
            new MarketBiasDataType(
                    MarketBiasEnum.BEARISH,
                    2,
                    "Bearish",
                    "",
                    ""
            ),
            new MarketBiasDataType(
                    MarketBiasEnum.NEUTRAL,
                    3,
                    "Neutral",
                    "",
                    ""
            )
    );

    public static final List<MarketStateDataType> MARKET_STATES = List.of(
            new MarketStateDataType(
                    MarketStateEnum.UPTREND,
                    1,
                    "Uptrend",
                    "",
                    ""
            ),

            new MarketStateDataType(
                    MarketStateEnum.DOWNTREND,
                    2,
                    "Downtrend",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.UPTREND_CORRECTION,
                    3,
                    "Uptrend Correction (Major Downtrend)",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.DOWNTREND_CORRECTION,
                    4,
                    "Downtrend Correction (Major Uptrend)",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.CONSOLIDATION,
                    5,
                    "Consolidation",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.HIGH_VOLATILITY,
                    6,
                    "High Volatility",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.LOW_VOLATILITY,
                    7,
                    "Low Volatility",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.HIGH_VOLUME,
                    8,
                    "High Volume",
                    "",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.LOW_VOLUME,
                    9,
                    "Low Volume",
                    "",
                    ""
            )
    );

    public static final List<EntryReasonDataType> ENTRY_REASONS = List.of(
            new EntryReasonDataType(
                    EntryReasonEnum.TREND_CONTINUATION,
                    1,
                    "Trend Continuation",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.TREND_REVERSAL,
                    2,
                    "Trend Reversal",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.LIQUIDITY_SWEEP,
                    3,
                    "Liquidity Sweep",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.RETEST,
                    4,
                    "Retest",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.BREAKOUT,
                    5,
                    "Breakout",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.PULLBACK,
                    6,
                    "Pullback",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.OB_MITIGATION,
                    7,
                    "OB Mitigation",
                    "",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.FVG_MITIGATION,
                    8,
                    "FVG Mitigation",
                    "",
                    ""
            )
    );

    public static final List<EntryConfirmationDataType> ENTRY_CONFIRMATIONS = List.of(
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.CANDLE_PATTERN,
                    1,
                    "Candle Pattern:",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.BOS,
                    2,
                    "BOS",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.CHOCH,
                    3,
                    "CHOCH",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.BREAKOUT,
                    4,
                    "Breakout",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.RETEST,
                    5,
                    "Retest",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.LIQUIDITY_SWEEP,
                    6,
                    "Liquidity Sweep",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.INDICATOR_CONFIRMATION,
                    7,
                    "Indicator Confirmation:",
                    "",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.NONE,
                    8,
                    "None (aggressive entry)",
                    "",
                    ""
            )
    );

    public static final List<EntryZoneTypeDataType> ENTRY_ZONE_TYPES = List.of(
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.OB,
                    1,
                    "OB",
                    "",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.FVG,
                    2,
                    "FVG",
                    "",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.LIQUIDITY,
                    3,
                    "Liquidity",
                    "",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.SUPPORT_RESISTANCE,
                    4,
                    "Support/Resistance",
                    "",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.SK_LEVELS,
                    5,
                    "SK Levels",
                    "",
                    ""
            )
    );

    public static final List<InvalidationConditionDataType> INVALIDATION_CONDITIONS = List.of(
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.CHOCH_AGAINST_DIRECTION,
                    1,
                    "CHOCH against direction",
                    "",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.BREAK_OF_OB,
                    2,
                    "Break of OB",
                    "",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.SWEEP_FAILURE,
                    3,
                    "Sweep Failure",
                    "",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.NEWS_VOLATILITY,
                    4,
                    "News Volatility",
                    "",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.VOLUME_DIVERGENCE,
                    5,
                    "Volume Divergence",
                    "",
                    ""
            )
    );

    public static final List<ProbabilityDataType> PROBABILITIES = List.of(
            new ProbabilityDataType(
                    ProbabilityEnum.HIGH,
                    1,
                    "High",
                    "",
                    ""
            ),
            new ProbabilityDataType(
                    ProbabilityEnum.MEDIUM,
                    1,
                    "Medium",
                    "",
                    ""
            ),
            new ProbabilityDataType(
                    ProbabilityEnum.LOW,
                    1,
                    "Low",
                    "",
                    ""
            )
    );

    public static final List<VolatilityDataType> VOLATILITIES = List.of(
            new VolatilityDataType(
                    VolatilityEnum.HIGH,
                    1,
                    "High",
                    "",
                    ""
            ),
            new VolatilityDataType(
                    VolatilityEnum.MEDIUM,
                    1,
                    "Medium",
                    "",
                    ""
            ),
            new VolatilityDataType(
                    VolatilityEnum.LOW,
                    1,
                    "Low",
                    "",
                    ""
            )
    );

    public static final List<ScalingRuleDataType> SCALING_RULES = List.of(
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_BOS,
                    1,
                    "Add on BOS",
                    "",
                    ""
            ),
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_BREAK,
                    2,
                    "Add on Break",
                    "",
                    ""
            ),
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_RETEST,
                    3,
                    "Add on Retest",
                    "",
                    ""
            )
    );

    public static final List<BoostRuleDataType> BOOST_RULES = List.of();

    public static final List<StopLossPlacementReasonDataType> STOP_LOSS_PLACEMENT_REASONS = List.of(
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.STRUCTURE,
                    1,
                    "Structure",
                    "",
                    ""
            ),
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.VOLATILITY_BASED,
                    2,
                    "Volatility Based",
                    "",
                    ""
            ),
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.PREVIOUS_HIGH,
                    3,
                    "Previous High",
                    "",
                    ""
            ),
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.PREVIOUS_LOW,
                    4,
                    "Previous Low",
                    "",
                    ""
            ),
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.OB_BOUNDARY,
                    5,
                    "OB Boundary",
                    "",
                    ""
            ),
            new StopLossPlacementReasonDataType(
                    StopLossPlacementReasonEnum.FVG_BOUNDARY,
                    6,
                    "FVG Boundary",
                    "",
                    ""
            )
    );


    public static List<TakeProfitPlacementReasonDataType> TAKE_PROFIT_PLACEMENT_REASONS = List.of(
            new TakeProfitPlacementReasonDataType(
                    TakeProfitPlacementReasonEnum.FVG_FILL,
                    1,
                    "FVG Fill",
                    "",
                    ""
            ),
            new TakeProfitPlacementReasonDataType(
                    TakeProfitPlacementReasonEnum.LIQUIDITY_SWEEP,
                    1,
                    "Liquidity Sweep",
                    "",
                    ""
            ),
            new TakeProfitPlacementReasonDataType(
                    TakeProfitPlacementReasonEnum.MAJOR_LEVEL,
                    1,
                    "Major Level",
                    "",
                    ""
            ),
            new TakeProfitPlacementReasonDataType(
                    TakeProfitPlacementReasonEnum.RISK_MULTIPLE_ACHIEVED,
                    1,
                    "Risk Multiple Achieved",
                    "",
                    ""
            ),
            new TakeProfitPlacementReasonDataType(
                    TakeProfitPlacementReasonEnum.OPPOSING_OB,
                    1,
                    "Opposing OB",
                    "",
                    ""
            )
    );

    public static List<EntryTypeDataType> ENTRY_TYPES = List.of(
            new EntryTypeDataType(
                    EntryTypeEnum.INSTANT,
                    1,
                    "Instant",
                    "",
                    ""

            ),
            new EntryTypeDataType(
                    EntryTypeEnum.LIMIT,
                    2,
                    "Limit",
                    "",
                    ""

            ),
            new EntryTypeDataType(
                    EntryTypeEnum.CONFIRMATION,
                    3,
                    "Confirmation",
                    "",
                    ""

            ),
            new EntryTypeDataType(
                    EntryTypeEnum.BREAKOUT,
                    4,
                    "Breakout",
                    "",
                    ""

            )
    );

    public static List<SessionDataType> SESSIONS = List.of(
            new SessionDataType(
                    SessionEnum.NEW_YORK_SESSION,
                    1,
                    "New York Session",
                    "",
                    ""
            ),
            new SessionDataType(
                    SessionEnum.LONDON_SESSION,
                    2,
                    "London Session",
                    "",
                    ""
            ),
            new SessionDataType(
                    SessionEnum.ASIAN_SESSION,
                    3,
                    "Asia Session",
                    "",
                    ""
            )
    );

    public static List<TradeTagDataType> TRADE_TAGS = List.of(
            new TradeTagDataType(
                    TradeTagEnum.TREND_TRADE,
                    1,
                    "Trend Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.REVERSAL_TRADE,
                    2,
                    "Reversal Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.LIQUIDITY_SWEEP_TRADE,
                    3,
                    "Liquidity Sweep Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.BREAKOUT_TRADE,
                    4,
                    "Breakout Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.FAILED_BREAKOUT_TRADE,
                    5,
                    "Failed Breakout Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.SMC_TRADE,
                    6,
                    "SMC Trade",
                    "",
                    ""
            ),
            new TradeTagDataType(
                    TradeTagEnum.NEWS_TRADE,
                    7,
                    "News Trade",
                    "",
                    ""
            )
    );

    public static List<TradeUpdateDataType> TRADE_UPDATES = List.of(
            new TradeUpdateDataType(
                    TradeUpdateEnum.MOVE_SL_TO_BE,
                    1,
                    "Move SL to BE",
                    "",
                    ""
            )
    );

    public static List<ExitReasonDataType> EXIT_REASONS = List.of(
            new ExitReasonDataType(
                    ExitReasonEnum.TP_HIT,
                    1,
                    "TP Hit",
                    "",
                    ""
            ),
            new ExitReasonDataType(
                    ExitReasonEnum.SL_HIT,
                    2,
                    "SL Hit",
                    "",
                    ""
            ),
            new ExitReasonDataType(
                    ExitReasonEnum.MANUAL_EXIT,
                    3,
                    "Manual Exit",
                    "",
                    ""
            ),
            new ExitReasonDataType(
                    ExitReasonEnum.STRUCTURE_BREAK,
                    4,
                    "Structure Break",
                    "",
                    ""
            ),
            new ExitReasonDataType(
                    ExitReasonEnum.PSYCHOLOGICAL_EXIT,
                    5,
                    "Psychological Exit",
                    "",
                    ""
            ),
            new ExitReasonDataType(
                    ExitReasonEnum.NEWS_IMPACT,
                    6,
                    "News Impact",
                    "",
                    ""
            )
    );
}