package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeSourceEnum {
    MANUAL(1),
    AUTO_IMPORTED(2);

    private final int index;

    TradeSourceEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
