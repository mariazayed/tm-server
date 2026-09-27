package trademind.tm_server.planner.dto;

import lombok.Data;
import trademind.tm_server.enums.PlanItemStatusEnum;

import java.time.LocalDate;

@Data
public class WeeklyTaskResponse {
    private Long id;
    private LocalDate weekStart;
    private String title;
    private String notes;
    private FocusAreaResponse focusArea;
    private Long monthlyObjectiveId;
    private String monthlyObjectiveTitle;
    private double estimatedHours;
    private PlanItemStatusEnum status;
    private Integer score;
    private Double actualHours;
    private String evaluationNotes;
    private Integer sortOrder;
}
