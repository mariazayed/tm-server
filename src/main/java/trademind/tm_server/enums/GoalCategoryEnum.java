package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GoalCategoryEnum {
    PROFIT(1),
    TRADE_QUALITY(2),
    DISCIPLINE(3),
    MANUAL(4);

    private final int index;

    GoalCategoryEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
