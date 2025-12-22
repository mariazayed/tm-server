package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.StopLossPlacementReasonEnum;

@Getter
public class StopLossPlacementReasonDataType extends BaseStaticDataType {
    StopLossPlacementReasonEnum id;

    public StopLossPlacementReasonDataType(StopLossPlacementReasonEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
