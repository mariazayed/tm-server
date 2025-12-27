package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EntryConfirmationEnum {
    CANDLE_PATTERN(1),
    BOS(2),
    CHOCH(3),
    BREAKOUT(4),
    RETEST(5),
    LIQUIDITY_SWEEP(6),
    INDICATOR_CONFIRMATION(7),
    NONE(8);

    private final int index;

    EntryConfirmationEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
