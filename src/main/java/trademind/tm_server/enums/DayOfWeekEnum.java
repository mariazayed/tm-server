package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DayOfWeekEnum {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final int index;

    DayOfWeekEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
