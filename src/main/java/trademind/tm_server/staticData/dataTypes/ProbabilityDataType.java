package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.ProbabilityEnum;

@Getter
public class ProbabilityDataType extends BaseStaticDataType {
    ProbabilityEnum id;

    public ProbabilityDataType(ProbabilityEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
