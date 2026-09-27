package trademind.tm_server.planner.dto;

import lombok.Data;

import java.time.LocalDate;

// PUT /planner/week -- the free-text note for one week. weekStart is
// normalised to that week's Monday. (Extra hours moved to their own
// entries: see WeekExtraRequest and /planner/week/extras.)
@Data
public class WeekExtrasRequest {
    private LocalDate weekStart;
    private String notes;
}
