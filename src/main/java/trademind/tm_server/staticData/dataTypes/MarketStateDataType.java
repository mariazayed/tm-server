package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.MarketStateEnum;

@Getter
public class MarketStateDataType extends BaseStaticDataType {
    MarketStateEnum id;

    public MarketStateDataType(MarketStateEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
