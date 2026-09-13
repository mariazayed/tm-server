package trademind.tm_server.decision.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import trademind.tm_server.enums.DecisionCategoryEnum;
import trademind.tm_server.enums.DecisionStatusEnum;

import java.time.LocalDateTime;

@Data
public class DecisionResponse {
    private Long id;
    private Integer decisionNumber;

    // Written back out in the same space-separated format DecisionModel
    // (Flutter) parses with DateTime.parse -- Dart accepts either 'T' or a
    // space, but this keeps request/response formatting consistent.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime decisionDate;
    private String title;
    private DecisionCategoryEnum category;
    private DecisionStatusEnum status;
    private String description;
    private String reasoning;
    private Integer relatedDecisionNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
