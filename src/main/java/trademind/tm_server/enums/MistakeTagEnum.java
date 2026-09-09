package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MistakeTagEnum {
    MOVED_SL_AGAINST_PLAN(1),
    OVERSIZED_POSITION(2),
    REVENGE_TRADE(3),
    FOMO_ENTRY(4),
    NO_CONFIRMATION_WAITED(5),
    IGNORED_NEWS_EVENT(6),
    EXITED_TOO_EARLY(7),
    EXITED_TOO_LATE(8),
    OVERTRADING(9),
    BROKE_STRICT_RULE(10),
    CHASED_ENTRY(11),
    NO_STOP_LOSS(12);

    private final int index;

    MistakeTagEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
