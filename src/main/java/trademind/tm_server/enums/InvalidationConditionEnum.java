package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum InvalidationConditionEnum {
    CHOCH_AGAINST_DIRECTION(1),
    BREAK_OF_OB(2),
    SWEEP_FAILURE(3),
    NEWS_VOLATILITY(4),
    VOLUME_DIVERGENCE(5);

    private final int index;

    InvalidationConditionEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
