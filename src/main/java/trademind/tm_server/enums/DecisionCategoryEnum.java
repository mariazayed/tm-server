package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DecisionCategoryEnum {
    TRADING(1),
    INVESTING(2),
    PERSONAL(3),
    SYSTEM(4),
    LEARNING(5),
    OTHERS(6);

    private final int index;

    DecisionCategoryEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
