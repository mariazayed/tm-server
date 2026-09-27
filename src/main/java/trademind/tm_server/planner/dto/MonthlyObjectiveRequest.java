package trademind.tm_server.planner.dto;

import lombok.Data;

// Create + partial update of a monthly objective's *plan* fields.
@Data
public class MonthlyObjectiveRequest {
    private Integer planYear;
    private Integer planMonth;
    private String title;
    private Long focusAreaId;
    private Double estimatedHours;
    private Integer sortOrder;
    private String notes;
}
