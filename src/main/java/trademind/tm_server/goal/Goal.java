package trademind.tm_server.goal;

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
import trademind.tm_server.enums.GoalCategoryEnum;
import trademind.tm_server.enums.GoalStatusEnum;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_GOALS)
public class Goal extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GoalCategoryEnum category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GoalStatusEnum status;

    // Target for PROFIT (currency amount) and TRADE_QUALITY (win rate %, 0-100).
    // Not used for DISCIPLINE or MANUAL, which are always measured against 100%.
    private Double targetValue;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    // Only used for MANUAL goals - the user updates this by hand, 0-100.
    private Integer manualProgressPct;
}
