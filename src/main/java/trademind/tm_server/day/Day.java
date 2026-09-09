package trademind.tm_server.day;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.enums.ScoreEnum;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_DAYS)
public class Day extends BaseEntity {
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeekEnum dayOfWeek;

    @Column(nullable = false)
    private boolean ended;

    @Column(nullable = false)
    private boolean noWork;

    @Column(nullable = false)
    private boolean weekend;

    @Enumerated(EnumType.STRING)
    private ScoreEnum psychologyRate;
}