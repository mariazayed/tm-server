package trademind.tm_server.planner.dto;

import lombok.Data;

import java.util.List;

// Everything the Planner's month view needs in one call.
@Data
public class MonthPlanResponse {
    private int year;
    private int month;

    private int workingDays;
    private double baseHours;
    private double extraHours;
    private double capacityHours;
    private double plannedHours;
    private double remainingHours;
    private boolean overCapacity;

    private List<MonthlyObjectiveResponse> objectives;
    private List<AreaAllocationResponse> areas;
    private EvaluationSummaryResponse evaluation;
    private List<WeekSnapshotResponse> weeks;
}
