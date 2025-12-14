package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.BoostRuleEnum;

public class BoostRuleDataType extends BaseStaticDataType{
    BoostRuleEnum id;

    public BoostRuleDataType(BoostRuleEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
