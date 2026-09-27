package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

// One row of the month view's "weeks" strip.
@Data
public class WeekSnapshotResponse {
    private LocalDate weekStart;
    private double capacityHours;
    private double plannedHours;
    private double actualHours;
    private int taskCount;
    private int doneCount;
    private double completionPct;
    private double extraHours;
    private int evaluatedCount;
    // Null when nothing that week has a score.
    private Double averageScore;
}
