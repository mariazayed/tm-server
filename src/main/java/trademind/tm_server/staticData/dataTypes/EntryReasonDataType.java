package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.EntryReasonEnum;

public class EntryReasonDataType extends BaseStaticDataType {
    EntryReasonEnum id;

    public EntryReasonDataType(EntryReasonEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
