package trademind.tm_server.planner.dto;

import lombok.Data;

// How much of a period's capacity one Focus Area takes, vs its max %.
@Data
public class AreaAllocationResponse {
    private FocusAreaResponse focusArea;
    private int itemCount;
    private double plannedHours;
    private double actualHours;
    private double plannedPct;
    private double actualPct;
    private double maxPercent;
    // Extra hours logged for this area (already included in allowedHours and,
    // since they are tasks, in plannedHours).
    private double extraHours;
    private double allowedHours;
    private boolean overLimit;
}
