package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ScalingRuleEnum {
    ADD_ON_BREAK(1),
    ADD_ON_RETEST(2),
    ADD_ON_BOS(3);

    private final int index;

    ScalingRuleEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
