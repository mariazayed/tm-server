package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GoalStatusEnum {
    ACTIVE(1),
    ACHIEVED(2),
    FAILED(3),
    ARCHIVED(4);

    private final int index;

    GoalStatusEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
