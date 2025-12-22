package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.MarketBiasEnum;

@Getter
public class MarketBiasDataType extends BaseStaticDataType {
    MarketBiasEnum id;

    public MarketBiasDataType(MarketBiasEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
