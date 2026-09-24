package trademind.tm_server.questionCategory.dto;

import lombok.Data;

@Data
public class NewQuestionCategoryRequest {
    private String name;
    private String notes;
}
