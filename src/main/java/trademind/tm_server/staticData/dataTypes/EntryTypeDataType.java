package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.EntryTypeEnum;

@Getter
public class EntryTypeDataType extends BaseStaticDataType {
    EntryTypeEnum id;

    public EntryTypeDataType(EntryTypeEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
