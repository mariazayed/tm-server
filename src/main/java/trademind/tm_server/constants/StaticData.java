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
                    "شراء"
            ),
            new TradeDirectionDataType(
                    TradeDirectionEnum.SHORT,
                    1,
                    "Sell",
                    "بيع"
            )
    );

    public static final List<MarketBiasDataType> MARKET_BIASES = List.of(
            new MarketBiasDataType(
                    MarketBiasEnum.BULLISH,
                    1,
                    "Bullish",
                    ""
            ),
            new MarketBiasDataType(
                    MarketBiasEnum.BEARISH,
                    2,
                    "Bearish",
                    ""
            ),
            new MarketBiasDataType(
                    MarketBiasEnum.NEUTRAL,
                    3,
                    "Neutral",
                    ""
            )
    );

    public static final List<MarketStateDataType> MARKET_STATES = List.of(
            new MarketStateDataType(
                    MarketStateEnum.UPTREND,
                    1,
                    "Uptrend",
                    ""
            ),

            new MarketStateDataType(
                    MarketStateEnum.DOWNTREND,
                    2,
                    "Downtrend",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.UPTREND_CORRECTION,
                    3,
                    "Uptrend Correction (Major Downtrend)",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.DOWNTREND_CORRECTION,
                    4,
                    "Downtrend Correction (Major Uptrend)",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.CONSOLIDATION,
                    5,
                    "Consolidation",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.HIGH_VOLATILITY,
                    6,
                    "High Volatility",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.LOW_VOLATILITY,
                    7,
                    "Low Volatility",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.HIGH_VOLUME,
                    8,
                    "High Volume",
                    ""
            ),
            new MarketStateDataType(
                    MarketStateEnum.LOW_VOLUME,
                    9,
                    "Low Volume",
                    ""
            )
    );

    public static final List<EntryReasonDataType> ENTRY_REASONS = List.of(
            new EntryReasonDataType(
                    EntryReasonEnum.TREND_CONTINUATION,
                    1,
                    "Trend Continuation",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.TREND_REVERSAL,
                    2,
                    "Trend Reversal",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.LIQUIDITY_SWEEP,
                    3,
                    "Liquidity Sweep",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.RETEST,
                    4,
                    "Retest",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.BREAKOUT,
                    5,
                    "Breakout",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.PULLBACK,
                    6,
                    "Pullback",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.OB_MITIGATION,
                    7,
                    "OB Mitigation",
                    ""
            ),
            new EntryReasonDataType(
                    EntryReasonEnum.FVG_MITIGATION,
                    8,
                    "FVG Mitigation",
                    ""
            )
    );

    public static final List<EntryConfirmationDataType> ENTRY_CONFIRMATIONS = List.of(
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.CANDLE_PATTERN,
                    1,
                    "Candle Pattern:",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.BOS,
                    2,
                    "BOS",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.CHOCH,
                    3,
                    "CHOCH",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.BREAKOUT,
                    4,
                    "Breakout",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.RETEST,
                    5,
                    "Retest",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.LIQUIDITY_SWEEP,
                    6,
                    "Liquidity Sweep",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.INDICATOR_CONFIRMATION,
                    7,
                    "Indicator Confirmation:",
                    ""
            ),
            new EntryConfirmationDataType(
                    EntryConfirmationEnum.NONE,
                    8,
                    "None (aggressive entry)",
                    ""
            )
    );

    public static final List<EntryZoneTypeDataType> ENTRY_ZONE_TYPES = List.of(
      new EntryZoneTypeDataType(
              EntryZoneTypeEnum.OB,
              1,
              "OB",
              ""
      ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.FVG,
                    2,
                    "FVG",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.LIQUIDITY,
                    3,
                    "Liquidity",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.SUPPORT_RESISTANCE,
                    4,
                    "Support/Resistance",
                    ""
            ),
            new EntryZoneTypeDataType(
                    EntryZoneTypeEnum.SK_LEVELS,
                    5,
                    "SK Levels",
                    ""
            )
            );

    public static final List<InvalidationConditionDataType> INVALIDATION_CONDITIONS = List.of(
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.CHOCH_AGAINST_DIRECTION,
                    1,
                    "CHOCH against direction",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.BREAK_OF_OB,
                    2,
                    "Break of OB",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.SWEEP_FAILURE,
                    3,
                    "Sweep Failure",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.NEWS_VOLATILITY,
                    4,
                    "News Volatility",
                    ""
            ),
            new InvalidationConditionDataType(
                    InvalidationConditionEnum.VOLUME_DIVERGENCE,
                    5,
                    "Volume Divergence",
                    ""
            )
    );

    public static final List<ProbabilityDataType> PROBABILITIES = List.of(
            new ProbabilityDataType(
                    ProbabilityEnum.HIGH,
                    1,
                    "High",
                    ""
            ),
            new ProbabilityDataType(
                    ProbabilityEnum.MEDIUM,
                    1,
                    "Medium",
                    ""
            ),
            new ProbabilityDataType(
                    ProbabilityEnum.LOW,
                    1,
                    "Low",
                    ""
            )
    );

    public static final List<VolatilityDataType> VOLATILITIES = List.of(
            new VolatilityDataType(
                    VolatilityEnum.HIGH,
                    1,
                    "High",
                    ""
            ),
            new VolatilityDataType(
                    VolatilityEnum.MEDIUM,
                    1,
                    "Medium",
                    ""
            ),
            new VolatilityDataType(
                    VolatilityEnum.LOW,
                    1,
                    "Low",
                    ""
            )
    );

    public static final List<ScalingRuleDataType> SCALING_RULES = List.of(
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_BOS,
                    1,
                    "Add on BOS",
                    ""
            ),
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_BREAK,
                    2,
                    "Add on Break",
                    ""
            ),
            new ScalingRuleDataType(
                    ScalingRuleEnum.ADD_ON_RETEST,
                    3,
                    "Add on Retest",
                    ""
            )
    );

    public static final List<BoostRuleDataType> BOOST_RULES = List.of();
}