package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.ScalingRuleEnum;

public class ScalingRuleDataType extends BaseStaticDataType {
    ScalingRuleEnum id;

    public ScalingRuleDataType(ScalingRuleEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
