package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.InvalidationConditionEnum;

@Getter
public class InvalidationConditionDataType extends BaseStaticDataType {
    InvalidationConditionEnum id;

    public InvalidationConditionDataType(InvalidationConditionEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
