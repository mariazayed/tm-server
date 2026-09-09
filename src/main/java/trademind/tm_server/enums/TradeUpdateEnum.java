package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeUpdateEnum {
    MOVE_SL_TO_BE(1),
    SL_MODIFIED(2),
    TP_MODIFIED(3),
    PARTIAL_CLOSE(4),
    SCALE_IN(5),
    SCALE_OUT(6),
    TRAILING_STOP_ADJUSTED(7);

    private final int index;

    TradeUpdateEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
