package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.StrategyTagEnum;

@Getter
public class StrategyTagDataType extends BaseStaticDataType {
    StrategyTagEnum id;

    public StrategyTagDataType(StrategyTagEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
