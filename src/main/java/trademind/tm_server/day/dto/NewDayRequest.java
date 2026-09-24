package trademind.tm_server.day.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.enums.ScoreEnum;

import java.time.LocalDateTime;

@Data
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

    @NotNull(message = "Required")
    @JsonProperty("isImportant")
    private boolean important;

    private ScoreEnum psychologyRate;

    private String notes;
}
