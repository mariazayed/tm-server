package trademind.tm_server.goal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.GoalCategoryEnum;
import trademind.tm_server.enums.GoalStatusEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private Long id;
    private String title;
    private GoalCategoryEnum category;
    private GoalStatusEnum status;
    private Double targetValue;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    private Integer manualProgressPct;
    private String notes;

    // Computed on every read - not persisted.
    private Double currentValue;
    private double progressPct;
    private boolean achieved;
    private long daysRemaining;
}
