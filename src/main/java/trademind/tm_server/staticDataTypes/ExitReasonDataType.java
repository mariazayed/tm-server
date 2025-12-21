package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.ExitReasonEnum;

public class ExitReasonDataType extends BaseStaticDataType {
    ExitReasonEnum id;

    public ExitReasonDataType(ExitReasonEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
