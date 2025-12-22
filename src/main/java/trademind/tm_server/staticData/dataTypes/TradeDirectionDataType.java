package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.TradeDirectionEnum;

public class TradeDirectionDataType extends BaseStaticDataType {
    TradeDirectionEnum id;

    public TradeDirectionDataType(TradeDirectionEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
