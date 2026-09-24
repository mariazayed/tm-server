package trademind.tm_server.questionAnswer.dto;

import lombok.Data;

@Data
public class UpdateQuestionAnswerRequest {
    private String question;
    private String answer;
    private Long categoryId;
    private String notes;
}
