package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.EntryTypeEnum;

public class EntryTypeDataType extends BaseStaticDataType {
    EntryTypeEnum id;

    public EntryTypeDataType(EntryTypeEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
