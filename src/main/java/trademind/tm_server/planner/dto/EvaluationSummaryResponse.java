package trademind.tm_server.planner.dto;

import lombok.Data;

@Data
public class EvaluationSummaryResponse {
    private int totalCount;
    private int doneCount;
    private int partialCount;
    private int missedCount;
    private int pendingCount;
    // Null when nothing has a score yet.
    private Double averageScore;
    // (done + 0.5 * partial) / total * 100
    private double completionPct;
    private double plannedHours;
    private double actualHours;
}
