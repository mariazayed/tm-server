package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WeekExtraResponse {
    private Long id;
    private LocalDate weekStart;
    private double hours;
    // Null = general extra time.
    private FocusAreaResponse focusArea;
    private String note;
}
