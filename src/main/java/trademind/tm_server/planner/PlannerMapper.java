package trademind.tm_server.planner;

import org.springframework.stereotype.Component;
import trademind.tm_server.planner.dto.FocusAreaResponse;
import trademind.tm_server.planner.dto.MonthlyObjectiveResponse;
import trademind.tm_server.planner.dto.PlannerSettingsDto;
import trademind.tm_server.planner.dto.WeekExtraResponse;
import trademind.tm_server.planner.dto.WeeklyTaskResponse;

// Hand-written rather than MapStruct: the responses carry nested/computed
// fields (objective title on a task, roll-ups on an objective) that are
// clearer set explicitly than via MapStruct expressions.
@Component
public class PlannerMapper {

    public FocusAreaResponse toResponse(FocusArea area) {
        if (area == null) return null;
        FocusAreaResponse r = new FocusAreaResponse();
        r.setId(area.getId());
        r.setName(area.getName());
        r.setColor(area.getColor());
        r.setIcon(area.getIcon());
        r.setMaxPercent(area.getMaxPercent());
        r.setSortOrder(area.getSortOrder());
        r.setArchived(area.isArchived());
        r.setNotes(area.getNotes());
        return r;
    }

    public WeeklyTaskResponse toResponse(WeeklyTask task) {
        WeeklyTaskResponse r = new WeeklyTaskResponse();
        r.setId(task.getId());
        r.setWeekStart(task.getWeekStart());
        r.setTitle(task.getTitle());
        r.setNotes(task.getNotes());
        r.setFocusArea(toResponse(task.getFocusArea()));
        MonthlyObjective objective = task.getMonthlyObjective();
        if (objective != null && objective.getDeletedAt() == null) {
            r.setMonthlyObjectiveId(objective.getId());
            r.setMonthlyObjectiveTitle(objective.getTitle());
        }
        r.setEstimatedHours(task.getEstimatedHours());
        r.setStatus(task.getStatus());
        r.setScore(task.getScore());
        r.setActualHours(task.getActualHours());
        r.setEvaluationNotes(task.getEvaluationNotes());
        r.setSortOrder(task.getSortOrder());
        return r;
    }

    // Roll-up fields are filled in by PlannerService.
    public MonthlyObjectiveResponse toResponse(MonthlyObjective objective) {
        MonthlyObjectiveResponse r = new MonthlyObjectiveResponse();
        r.setId(objective.getId());
        r.setPlanYear(objective.getPlanYear());
        r.setPlanMonth(objective.getPlanMonth());
        r.setTitle(objective.getTitle());
        r.setNotes(objective.getNotes());
        r.setFocusArea(toResponse(objective.getFocusArea()));
        r.setEstimatedHours(objective.getEstimatedHours());
        r.setStatus(objective.getStatus());
        r.setScore(objective.getScore());
        r.setActualHours(objective.getActualHours());
        r.setEvaluationNotes(objective.getEvaluationNotes());
        r.setSortOrder(objective.getSortOrder());
        return r;
    }

    public WeekExtraResponse toResponse(PlannerWeekExtra extra) {
        WeekExtraResponse r = new WeekExtraResponse();
        r.setId(extra.getId());
        r.setWeekStart(extra.getWeekStart());
        r.setHours(extra.getHours());
        FocusArea area = extra.getFocusArea();
        r.setFocusArea(area != null ? toResponse(area) : null);
        r.setNote(extra.getNotes());
        return r;
    }

    public PlannerSettingsDto toResponse(PlannerSettings s) {
        PlannerSettingsDto r = new PlannerSettingsDto();
        r.setMondayHours(s.getMondayHours());
        r.setTuesdayHours(s.getTuesdayHours());
        r.setWednesdayHours(s.getWednesdayHours());
        r.setThursdayHours(s.getThursdayHours());
        r.setFridayHours(s.getFridayHours());
        r.setSaturdayHours(s.getSaturdayHours());
        r.setSundayHours(s.getSundayHours());
        r.setWeeklyHours(s.weeklyHours());
        r.setWorkingDays(s.weeklyWorkingDays());
        return r;
    }
}
