package trademind.tm_server.staticData.dataTypes;

import trademind.tm_server.enums.SessionEnum;

public class SessionDataType extends BaseStaticDataType {
    SessionEnum id;

    public SessionDataType(SessionEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
