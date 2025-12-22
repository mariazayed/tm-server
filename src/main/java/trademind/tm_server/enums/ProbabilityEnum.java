package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProbabilityEnum {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int index;

    ProbabilityEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
