package trademind.tm_server.staticDataTypes;

import trademind.tm_server.enums.EntryZoneTypeEnum;

public class EntryZoneTypeDataType extends BaseStaticDataType{
    EntryZoneTypeEnum id;

    public EntryZoneTypeDataType(EntryZoneTypeEnum id, int order, String enName, String arName) {
        super(order, enName, arName);
        this.id = id;
    }
}
