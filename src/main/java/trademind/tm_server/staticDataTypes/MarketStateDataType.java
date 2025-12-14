package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.MarketStateEnum;

public class MarketStateDataType extends BaseStaticDataType{
    MarketStateEnum id;

    public MarketStateDataType(MarketStateEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
