package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.ScalingRuleEnum;

public class ScalingRuleDataType extends BaseStaticDataType{
    ScalingRuleEnum id;

    public ScalingRuleDataType(ScalingRuleEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
