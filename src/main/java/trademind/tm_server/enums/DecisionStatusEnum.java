package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DecisionStatusEnum {
    ACTIVE(1),
    SUPERSEDED(2),
    REVERSED(3),
    COMPLETED(4);

    private final int index;

    DecisionStatusEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
