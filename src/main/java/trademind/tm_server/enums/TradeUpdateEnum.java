package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeUpdateEnum {
    MOVE_SL_TO_BE(1);

    private final int index;

    TradeUpdateEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
