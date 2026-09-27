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

import java.time.DayOfWeek;

// Single-row table: working hours per weekday. Weekly capacity is the sum
// of all seven; a weekday with hours > 0 counts as a working day.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_PLANNER_SETTINGS)
public class PlannerSettings extends BaseEntity {

    @Column(nullable = false)
    private double mondayHours;

    @Column(nullable = false)
    private double tuesdayHours;

    @Column(nullable = false)
    private double wednesdayHours;

    @Column(nullable = false)
    private double thursdayHours;

    @Column(nullable = false)
    private double fridayHours;

    @Column(nullable = false)
    private double saturdayHours;

    @Column(nullable = false)
    private double sundayHours;

    public double hoursFor(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> mondayHours;
            case TUESDAY -> tuesdayHours;
            case WEDNESDAY -> wednesdayHours;
            case THURSDAY -> thursdayHours;
            case FRIDAY -> fridayHours;
            case SATURDAY -> saturdayHours;
            case SUNDAY -> sundayHours;
        };
    }

    public double weeklyHours() {
        double total = 0;
        for (DayOfWeek day : DayOfWeek.values()) total += hoursFor(day);
        return total;
    }

    public int weeklyWorkingDays() {
        int count = 0;
        for (DayOfWeek day : DayOfWeek.values()) if (hoursFor(day) > 0) count++;
        return count;
    }
}
