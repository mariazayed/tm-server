package trademind.tm_server.day;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.trade.Trade;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_DAYS)
public class Day extends BaseEntity {
    @Column(nullable = false)
    private DayOfWeekEnum dayOfWeek;

    @Column(nullable = false)
    private boolean isEnded;

    @Column(nullable = false)
    private boolean noWork;

    @Column(nullable = false)
    private boolean weekend;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;
}