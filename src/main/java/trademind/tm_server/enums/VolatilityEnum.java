package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum VolatilityEnum {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int index;

    VolatilityEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
