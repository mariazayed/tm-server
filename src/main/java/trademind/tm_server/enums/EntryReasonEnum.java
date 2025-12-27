package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EntryReasonEnum {
    TREND_CONTINUATION(1),
    TREND_REVERSAL(2),
    LIQUIDITY_SWEEP(3),
    RETEST(4),
    BREAKOUT(5),
    PULLBACK(6),
    OB_MITIGATION(7),
    FVG_MITIGATION(8);

    private final int index;

    EntryReasonEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
