package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.VolatilityEnum;

public class VolatilityDataType extends BaseStaticDataType{
    VolatilityEnum id;

    public VolatilityDataType(VolatilityEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
