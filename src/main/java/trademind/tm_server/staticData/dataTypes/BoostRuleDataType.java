package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.BoostRuleEnum;

@Getter
public class BoostRuleDataType extends BaseStaticDataType {
    BoostRuleEnum id;

    public BoostRuleDataType(BoostRuleEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
