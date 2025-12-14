package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.TradeDirectionEnum;

public class TradeDirectionDataType extends BaseStaticDataType{
    TradeDirectionEnum id;

    public TradeDirectionDataType(TradeDirectionEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
