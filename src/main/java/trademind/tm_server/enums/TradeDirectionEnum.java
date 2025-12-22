package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeDirectionEnum {
    LONG(1),
    SHORT(2);

    private final int index;

    TradeDirectionEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
