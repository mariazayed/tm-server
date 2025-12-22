package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SessionEnum {
    NEW_YORK_SESSION(2),
    LONDON_SESSION(2),
    ASIAN_SESSION(3);

    private final int index;

    SessionEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
