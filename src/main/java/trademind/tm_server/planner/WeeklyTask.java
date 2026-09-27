package trademind.tm_server.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.PlanItemStatusEnum;

import java.time.LocalDate;

// A task planned for one week (weekStart is always a Monday), optionally
// linked to a monthly objective it contributes to.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_WEEKLY_TASKS)
public class WeeklyTask extends BaseEntity {

    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_area_id", nullable = false)
    private FocusArea focusArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_objective_id")
    private MonthlyObjective monthlyObjective;

    @Column(nullable = false)
    private double estimatedHours;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanItemStatusEnum status = PlanItemStatusEnum.PLANNED;

    // Evaluation: 1-5, null until evaluated.
    private Integer score;

    private Double actualHours;

    @Column(columnDefinition = "text")
    private String evaluationNotes;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
