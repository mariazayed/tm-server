package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.EntryReasonEnum;

public class EntryReasonDataType extends BaseStaticDataType{
    EntryReasonEnum id;

    public EntryReasonDataType(EntryReasonEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
