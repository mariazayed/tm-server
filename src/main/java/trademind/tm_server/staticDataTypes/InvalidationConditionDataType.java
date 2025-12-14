package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.InvalidationConditionEnum;

public class InvalidationConditionDataType extends BaseStaticDataType{
    InvalidationConditionEnum id;

    public InvalidationConditionDataType(InvalidationConditionEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
