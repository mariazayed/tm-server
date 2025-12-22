package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EntryZoneTypeEnum {
    OB(1),
    FVG(2),
    LIQUIDITY(3),
    SUPPORT_RESISTANCE(4),
    SK_LEVELS(5);

    private final int index;

    EntryZoneTypeEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}
