package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum StopLossPlacementReasonEnum {
    STRUCTURE(1),
    VOLATILITY_BASED(2),
    PREVIOUS_HIGH(3),
    PREVIOUS_LOW(4),
    OB_BOUNDARY(5),
    FVG_BOUNDARY(6);

    private final int index;

    StopLossPlacementReasonEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
