package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum StrategyTagEnum {

    TREND_TRADE(1),
    REVERSAL_TRADE(2),
    LIQUIDITY_SWEEP_TRADE(3),
    BREAKOUT_TRADE(4),
    FAILED_BREAKOUT_TRADE(5),
    NEWS_TRADE(6),
    SMC_TRADE(7);

    private final int value;

    StrategyTagEnum(int value) {
        this.value = value;
    }

    // ---------- SERIALIZATION (Java -> JSON)
    @JsonValue
    public int toJson() {
        return value;
    }

    // ---------- DESERIALIZATION (JSON -> Java)
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StrategyTagEnum fromJson(int value) {
        for (StrategyTagEnum e : StrategyTagEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        throw new IllegalArgumentException(
                "Invalid StrategyTagEnum value: " + value
        );
    }
}
