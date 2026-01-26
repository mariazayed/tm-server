package trademind.tm_server.day.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDayRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    private DayOfWeekEnum dayOfWeek;

    private boolean ended;

    private boolean noWork;

    private boolean weekend;

    private String notes;
}
