package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.TradeTagEnum;

@Getter
public class TradeTagDataType extends BaseStaticDataType {
    TradeTagEnum id;

    public TradeTagDataType(TradeTagEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
