package trademind.tm_server.planner.dto;

import lombok.Data;
import trademind.tm_server.enums.PlanItemStatusEnum;

// PUT .../{id}/evaluation -- replaces the whole evaluation (nulls clear).
// Sending status PLANNED with nulls resets an item to "not evaluated".
@Data
public class EvaluationRequest {
    private PlanItemStatusEnum status;
    private Integer score;
    private Double actualHours;
    private String evaluationNotes;
}
