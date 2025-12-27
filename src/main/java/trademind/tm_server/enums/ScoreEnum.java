package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ScoreEnum {
    TERRIBLE(1),
    VERY_POOR(2),
    POOR(3),
    BELOW_AVERAGE(4),
    AVERAGE(5),
    ABOVE_AVERAGE(6),
    GOOD(7),
    VERY_GOOD(8),
    EXCELLENT(9),
    WORLD_CLASS(10);

    private final int index;

    ScoreEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
