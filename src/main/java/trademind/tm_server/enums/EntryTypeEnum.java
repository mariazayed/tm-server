package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EntryTypeEnum {
    INSTANT(1),
    LIMIT(2),
    CONFIRMATION(3),
    BREAKOUT(4);

    private final int index;

    EntryTypeEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
