package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.EntryConfirmationEnum;

@Getter
public class EntryConfirmationDataType extends BaseStaticDataType {
    EntryConfirmationEnum id;

    public EntryConfirmationDataType(EntryConfirmationEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
