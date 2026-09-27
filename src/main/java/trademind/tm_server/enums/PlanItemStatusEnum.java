package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

// Outcome of a planner item (weekly task / monthly objective). PLANNED means
// "not evaluated yet"; the other three are the evaluation outcomes.
@Getter
public enum PlanItemStatusEnum {
    PLANNED(1),
    DONE(2),
    PARTIAL(3),
    MISSED(4);

    private final int index;

    PlanItemStatusEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return index;
    }
}
