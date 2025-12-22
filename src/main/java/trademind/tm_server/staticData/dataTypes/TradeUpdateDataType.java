package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.TradeUpdateEnum;

public class TradeUpdateDataType extends BaseStaticDataType {
    TradeUpdateEnum id;

    public TradeUpdateDataType(TradeUpdateEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
