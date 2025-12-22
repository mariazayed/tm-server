package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.SessionEnum;

@Getter
public class SessionDataType extends BaseStaticDataType {
    SessionEnum id;

    public SessionDataType(SessionEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
