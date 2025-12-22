package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;

@Getter
public class BaseStaticDataType {
    int order;
    String enName;
    String arName;
    String description;

    public BaseStaticDataType(int order, String enName, String arName, String description) {
        this.order = order;
        this.enName = enName;
        this.arName = arName;
        this.description = description;
    }
}
