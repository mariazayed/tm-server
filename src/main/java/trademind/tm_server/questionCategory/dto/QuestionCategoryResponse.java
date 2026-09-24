package trademind.tm_server.questionCategory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionCategoryResponse {
    private Long id;
    private String name;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
