package trademind.tm_server.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

// One block of extra working hours in a week, on top of the weekday
// settings. focusArea == null means general time (raises capacity, so every
// area's max % allowance scales with it); a set focusArea means the hours
// are dedicated to that area and are added straight to its allowance.
// BaseEntity.notes holds the reason ("weekend backtesting session").
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_PLANNER_WEEK_EXTRAS)
public class PlannerWeekExtra extends BaseEntity {

    // Always a Monday.
    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private double hours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_area_id")
    private FocusArea focusArea;
}
