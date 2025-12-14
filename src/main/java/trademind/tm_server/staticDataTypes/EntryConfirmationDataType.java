package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.EntryConfirmationEnum;

public class EntryConfirmationDataType extends BaseStaticDataType{
    EntryConfirmationEnum id;

    public EntryConfirmationDataType(EntryConfirmationEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
