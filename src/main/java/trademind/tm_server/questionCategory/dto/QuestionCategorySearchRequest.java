package trademind.tm_server.questionCategory.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionCategorySearchRequest extends BaseSearchRequest {
    private String name;
}
