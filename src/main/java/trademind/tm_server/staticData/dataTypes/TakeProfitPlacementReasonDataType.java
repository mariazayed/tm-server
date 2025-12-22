package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.TakeProfitPlacementReasonEnum;

public class TakeProfitPlacementReasonDataType extends BaseStaticDataType {
    TakeProfitPlacementReasonEnum id;

    public TakeProfitPlacementReasonDataType(TakeProfitPlacementReasonEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
