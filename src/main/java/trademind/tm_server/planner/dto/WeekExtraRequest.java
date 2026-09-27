package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

// POST/PUT /planner/week/extras -- one block of extra hours.
// focusAreaId null = general time. On update, clearFocusArea=true turns a
// dedicated block back into general time.
@Data
public class WeekExtraRequest {
    private LocalDate weekStart;
    private Double hours;
    private Long focusAreaId;
    private Boolean clearFocusArea;
    private String note;
}
