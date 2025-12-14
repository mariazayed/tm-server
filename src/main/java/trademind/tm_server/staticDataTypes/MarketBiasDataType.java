package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.MarketBiasEnum;

public class MarketBiasDataType extends BaseStaticDataType{
    MarketBiasEnum id;

    public MarketBiasDataType(MarketBiasEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
