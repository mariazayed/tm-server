package trademind.tm_server.questionAnswer.dto;

import lombok.Data;
import trademind.tm_server.questionCategory.dto.QuestionCategoryResponse;

import java.time.LocalDateTime;

@Data
public class QuestionAnswerResponse {
    private Long id;
    private String question;
    private String answer;
    private QuestionCategoryResponse category;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
