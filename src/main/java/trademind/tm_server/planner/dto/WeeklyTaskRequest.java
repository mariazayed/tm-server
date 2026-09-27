package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

// Create + partial update of a weekly task's *plan* fields. Evaluation
// fields go through EvaluationRequest instead.
@Data
public class WeeklyTaskRequest {
    private LocalDate weekStart;
    private String title;
    private Long focusAreaId;
    private Long monthlyObjectiveId;
    // On update: true removes the objective link (a null monthlyObjectiveId
    // alone means "leave unchanged").
    private Boolean unlinkObjective;
    private Double estimatedHours;
    // Create only: true = extra working hours (see WeeklyTask.extra).
    private Boolean extra;
    // Time actually spent so far -- can be logged any time (not only at
    // evaluation) and may exceed estimatedHours. On update, null = leave
    // unchanged; clearActualHours=true removes it.
    private Double actualHours;
    private Boolean clearActualHours;
    private Integer sortOrder;
    private String notes;
}
