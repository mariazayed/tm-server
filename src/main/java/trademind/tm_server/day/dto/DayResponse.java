package trademind.tm_server.day.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.enums.ScoreEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayResponse {
    private int id;
    private String notes;
    private DayOfWeekEnum dayOfWeek;
    private ScoreEnum psychologyRate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;
    private boolean ended;
    private boolean noWork;
    private boolean weekend;
}
