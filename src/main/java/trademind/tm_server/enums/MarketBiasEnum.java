package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MarketBiasEnum {
    BULLISH(1),
    BEARISH(2),
    NEUTRAL(3);

    private final int index;

    MarketBiasEnum(int index) {
        this.index = index;
    }

//    public static MarketBiasEnum fromIndex(int index) {
//        for (MarketBiasEnum e : values()) {
//            if (e.index == index) {
//                return e;
//            }
//        }
//        throw new IllegalArgumentException("Invalid index: " + index);
//    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
