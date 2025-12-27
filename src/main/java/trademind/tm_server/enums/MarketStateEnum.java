package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MarketStateEnum {
    UPTREND(1),
    DOWNTREND(2),
    DOWNTREND_CORRECTION(3),
    UPTREND_CORRECTION(4),
    CONSOLIDATION(5),
    HIGH_VOLATILITY(6),
    LOW_VOLATILITY(7),
    HIGH_VOLUME(8),
    LOW_VOLUME(9);

    private final int index;

    MarketStateEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
