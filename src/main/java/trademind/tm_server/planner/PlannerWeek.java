package trademind.tm_server.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

import java.time.LocalDate;

// Per-week record holding the free-text week note (BaseEntity.notes). Extra
// hours moved to PlannerWeekExtra (one row per block, optionally tagged with
// a Focus Area) in V20. Only exists once the user writes a note.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_PLANNER_WEEKS)
public class PlannerWeek extends BaseEntity {

    // Always a Monday.
    @Column(nullable = false, unique = true)
    private LocalDate weekStart;
}
