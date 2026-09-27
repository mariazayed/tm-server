package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Everything the Planner's week view needs in one call.
@Data
public class WeekPlanResponse {
    private LocalDate weekStart;
    private LocalDate weekEnd;

    private int workingDays;
    private double baseHours;
    private double extraHours;
    // Extra-hours entries for this week (general + per Focus Area).
    private List<WeekExtraResponse> extras;
    private String notes;
    private double capacityHours;
    private double plannedHours;
    private double remainingHours;
    private boolean overCapacity;

    private List<WeeklyTaskResponse> tasks;
    private List<AreaAllocationResponse> areas;
    private EvaluationSummaryResponse evaluation;

    // Objectives of the month(s) this week touches -- the pool a task can
    // be linked to.
    private List<MonthlyObjectiveResponse> objectives;
}
