package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeTypeEnum {
    SCALPING(1),
    INTRADAY(2),
    SWING(3);

    private final int index;

    TradeTypeEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
