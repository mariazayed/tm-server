package trademind.tm_server.day.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewDayRequest {
    @NotNull(message = "Required")
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Required")
    private boolean isEnded;

    @NotNull(message = "Required")
    private boolean noWork;

    @NotNull(message = "Required")
    private boolean weekEnd;

    private String notes;
}
