package trademind.tm_server.questionCategory.dto;

import lombok.Data;

@Data
public class UpdateQuestionCategoryRequest {
    private String name;
    private String notes;
}
