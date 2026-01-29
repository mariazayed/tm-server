package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CapitalReasonEnum {
    PROFIT(1),
    LOSS(2),
    WITHDRAW(3),
    DEPOSIT(4);

    private final int index;

    CapitalReasonEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
