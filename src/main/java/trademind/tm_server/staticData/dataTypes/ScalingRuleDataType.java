package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.ScalingRuleEnum;

@Getter
public class ScalingRuleDataType extends BaseStaticDataType {
    ScalingRuleEnum id;

    public ScalingRuleDataType(ScalingRuleEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
