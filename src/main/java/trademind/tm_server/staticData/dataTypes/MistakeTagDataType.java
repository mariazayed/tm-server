package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.MistakeTagEnum;

@Getter
public class MistakeTagDataType extends BaseStaticDataType {
    MistakeTagEnum id;

    public MistakeTagDataType(MistakeTagEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
