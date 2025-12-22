package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.EntryZoneTypeEnum;

@Getter
public class EntryZoneTypeDataType extends BaseStaticDataType {
    EntryZoneTypeEnum id;

    public EntryZoneTypeDataType(EntryZoneTypeEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
