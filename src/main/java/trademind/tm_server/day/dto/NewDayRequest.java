package trademind.tm_server.day.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewDayRequest {
    @NotNull(message = "Required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @NotNull(message = "Required")
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Required")
    private boolean ended;

    @NotNull(message = "Required")
    private boolean noWork;

    @NotNull(message = "Required")
    private boolean weekend;

    private String notes;
}
