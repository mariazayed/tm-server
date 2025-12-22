package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.EntryConfirmationEnum;

public class EntryConfirmationDataType extends BaseStaticDataType {
    EntryConfirmationEnum id;

    public EntryConfirmationDataType(EntryConfirmationEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
