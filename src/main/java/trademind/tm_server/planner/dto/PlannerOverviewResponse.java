package trademind.tm_server.planner.dto;

import lombok.Data;

import java.util.List;

// GET /planner/overview -- everything the Home dashboard's Planner section
// needs in one call.
@Data
public class PlannerOverviewResponse {
    private WeekPlanResponse currentWeek;
    private MonthPlanResponse currentMonth;

    // Last N weeks, oldest first, ending with the current week.
    private List<WeekSnapshotResponse> history;

    // Consecutive weeks (ending with the current one) that have at least one
    // task planned.
    private int planningStreak;

    // Consecutive past weeks (ending with last week) where every task got
    // evaluated.
    private int evaluationStreak;

    // Days left in the current week, today included (Mon = 7 ... Sun = 1).
    private int daysLeftInWeek;

    // Share of total planned hours over the history window per Focus Area,
    // with actual hours alongside.
    private List<AreaAllocationResponse> areaMix;
}
