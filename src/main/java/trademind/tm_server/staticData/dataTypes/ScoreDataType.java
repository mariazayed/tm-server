package trademind.tm_server.staticData.dataTypes;

import lombok.Getter;
import trademind.tm_server.enums.ScoreEnum;

@Getter
public class ScoreDataType extends BaseStaticDataType {
    ScoreEnum id;

    public ScoreDataType(ScoreEnum id, int order, String enName, String arName, String description) {
        super(order, enName, arName, description);
        this.id = id;
    }
}
