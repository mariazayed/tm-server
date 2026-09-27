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
    // Extra hours dedicated to this area (already included in allowedHours).
    private double extraHours;
    private double allowedHours;
    private boolean overLimit;
}
