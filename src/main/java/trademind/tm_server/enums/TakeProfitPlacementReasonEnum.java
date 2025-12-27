package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TakeProfitPlacementReasonEnum {
    FVG_FILL(1),
    LIQUIDITY_SWEEP(2),
    MAJOR_LEVEL(3),
    RISK_MULTIPLE_ACHIEVED(4),
    OPPOSING_OB(5);

    private final int index;

    TakeProfitPlacementReasonEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
