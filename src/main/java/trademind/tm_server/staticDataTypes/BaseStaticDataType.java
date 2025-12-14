package trademind.tm_server.staticDataTypes;

public class BaseStaticDataType {
    int order;
    String enName;
    String arName;

    public BaseStaticDataType(int order, String enName, String arName){
        this.order = order;
        this.enName = enName;
        this.arName = arName;
    }
}
