package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ExitReasonEnum {
    TP_HIT(1),
    SL_HIT(2),
    MANUAL_EXIT(3),
    STRUCTURE_BREAK(4),
    PSYCHOLOGICAL_EXIT(5),
    NEWS_IMPACT(6);

    private final int index;

    ExitReasonEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
