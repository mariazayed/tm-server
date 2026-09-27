package trademind.tm_server.planner.dto;

import lombok.Data;
import trademind.tm_server.enums.PlanItemStatusEnum;

@Data
public class MonthlyObjectiveResponse {
    private Long id;
    private Integer planYear;
    private Integer planMonth;
    private String title;
    private String notes;
    private FocusAreaResponse focusArea;
    private double estimatedHours;
    private PlanItemStatusEnum status;
    private Integer score;
    private Double actualHours;
    private String evaluationNotes;
    private Integer sortOrder;

    // Roll-up from linked weekly tasks -- computed on read, never stored.
    private int linkedTaskCount;
    private int linkedDoneCount;
    private double linkedPlannedHours;
    // Hours credited by linked tasks: actual hours when entered, otherwise
    // estimated hours for DONE tasks and half of them for PARTIAL ones.
    private double linkedCreditedHours;
    private double progressPct;
}
