package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

// POST/PUT /planner/week/extras -- one block of extra hours for a Focus
// Area. focusAreaId is required on create (same as a task); on update it may
// be omitted to keep the current area. clearFocusArea is ignored (kept only
// so older clients don't fail to deserialize).
@Data
public class WeekExtraRequest {
    private LocalDate weekStart;
    private Double hours;
    private Long focusAreaId;
    private Boolean clearFocusArea;
    private String note;
}
