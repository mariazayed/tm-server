package trademind.tm_server.planner.dto;

import lombok.Data;

// Request and response for GET/PUT /planner/settings. weeklyHours and
// workingDays are computed on read and ignored on write.
@Data
public class PlannerSettingsDto {
    private Double mondayHours;
    private Double tuesdayHours;
    private Double wednesdayHours;
    private Double thursdayHours;
    private Double fridayHours;
    private Double saturdayHours;
    private Double sundayHours;

    private double weeklyHours;
    private int workingDays;
}
