package trademind.tm_server.planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.planner.dto.EvaluationRequest;
import trademind.tm_server.planner.dto.FocusAreaRequest;
import trademind.tm_server.planner.dto.FocusAreaResponse;
import trademind.tm_server.planner.dto.MonthPlanResponse;
import trademind.tm_server.planner.dto.MonthlyObjectiveRequest;
import trademind.tm_server.planner.dto.MonthlyObjectiveResponse;
import trademind.tm_server.planner.dto.PlannerOverviewResponse;
import trademind.tm_server.planner.dto.PlannerSettingsDto;
import trademind.tm_server.planner.dto.WeekExtraRequest;
import trademind.tm_server.planner.dto.WeekExtrasRequest;
import trademind.tm_server.planner.dto.WeekPlanResponse;
import trademind.tm_server.planner.dto.WeeklyTaskRequest;
import trademind.tm_server.planner.dto.WeeklyTaskResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * All Planner endpoints under /api/v1/planner:
 *
 *   GET    /settings                      working hours per weekday
 *   PUT    /settings
 *   GET    /focusAreas                    CRUD for Focus Areas
 *   POST   /focusAreas
 *   PUT    /focusAreas/{id}
 *   DELETE /focusAreas/{id}
 *   GET    /week?weekStart=yyyy-MM-dd     full week view (capacity, tasks, allocation, evaluation)
 *   PUT    /week                          note for a week
 *   POST   /week/extras                   add extra hours (general or for one Focus Area)
 *   PUT    /week/extras/{id}
 *   DELETE /week/extras/{id}               (extras endpoints return the refreshed week)
 *   POST   /week/carryOver?weekStart=     copy last week's unfinished tasks
 *   GET    /month?year=&month=            full month view
 *   GET    /overview?weeks=8              Home dashboard summary (week, month, history, streaks)
 *   POST   /tasks                         weekly task CRUD
 *   PUT    /tasks/{id}
 *   PUT    /tasks/{id}/evaluation
 *   DELETE /tasks/{id}
 *   POST   /objectives                    monthly objective CRUD
 *   PUT    /objectives/{id}
 *   PUT    /objectives/{id}/evaluation
 *   DELETE /objectives/{id}
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_PLANNER)
public class PlannerController {

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private PlannerItemService itemService;

    @Autowired
    private FocusAreaService focusAreaService;

    // ---------------------------------------------------------------- settings

    @GetMapping("/settings")
    public PlannerSettingsDto getSettings() {
        return plannerService.getSettings();
    }

    @PutMapping("/settings")
    public PlannerSettingsDto updateSettings(@RequestBody PlannerSettingsDto request) {
        return plannerService.updateSettings(request);
    }

    // ------------------------------------------------------------- focus areas

    @GetMapping("/focusAreas")
    public List<FocusAreaResponse> getFocusAreas() {
        return focusAreaService.getAll();
    }

    @PostMapping("/focusAreas")
    public FocusAreaResponse createFocusArea(@RequestBody FocusAreaRequest request) {
        return focusAreaService.create(request);
    }

    @PutMapping("/focusAreas/{id}")
    public FocusAreaResponse updateFocusArea(@PathVariable Long id, @RequestBody FocusAreaRequest request) {
        return focusAreaService.update(id, request);
    }

    @DeleteMapping("/focusAreas/{id}")
    public ResponseEntity<Void> deleteFocusArea(@PathVariable Long id) {
        focusAreaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------- week

    @GetMapping("/week")
    public WeekPlanResponse getWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return plannerService.getWeekPlan(weekStart);
    }

    @PutMapping("/week")
    public WeekPlanResponse updateWeek(@RequestBody WeekExtrasRequest request) {
        return plannerService.updateWeekNotes(request);
    }

    @PostMapping("/week/extras")
    public WeekPlanResponse addExtra(@RequestBody WeekExtraRequest request) {
        return plannerService.addExtra(request);
    }

    @PutMapping("/week/extras/{id}")
    public WeekPlanResponse updateExtra(@PathVariable Long id, @RequestBody WeekExtraRequest request) {
        return plannerService.updateExtra(id, request);
    }

    @DeleteMapping("/week/extras/{id}")
    public WeekPlanResponse deleteExtra(@PathVariable Long id) {
        return plannerService.deleteExtra(id);
    }

    @PostMapping("/week/carryOver")
    public WeekPlanResponse carryOver(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return plannerService.carryOverUnfinished(weekStart);
    }

    // ------------------------------------------------------------------- month

    @GetMapping("/month")
    public MonthPlanResponse getMonth(@RequestParam int year, @RequestParam int month) {
        return plannerService.getMonthPlan(year, month);
    }

    // ---------------------------------------------------------------- overview

    @GetMapping("/overview")
    public PlannerOverviewResponse getOverview(@RequestParam(defaultValue = "8") int weeks) {
        return plannerService.getOverview(weeks);
    }

    // ------------------------------------------------------------ weekly tasks

    @PostMapping("/tasks")
    public WeeklyTaskResponse createTask(@RequestBody WeeklyTaskRequest request) {
        return itemService.createTask(request);
    }

    @PutMapping("/tasks/{id}")
    public WeeklyTaskResponse updateTask(@PathVariable Long id, @RequestBody WeeklyTaskRequest request) {
        return itemService.updateTask(id, request);
    }

    @PutMapping("/tasks/{id}/evaluation")
    public WeeklyTaskResponse evaluateTask(@PathVariable Long id, @RequestBody EvaluationRequest request) {
        return itemService.evaluateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        itemService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------ monthly objectives

    @PostMapping("/objectives")
    public MonthlyObjectiveResponse createObjective(@RequestBody MonthlyObjectiveRequest request) {
        return itemService.createObjective(request);
    }

    @PutMapping("/objectives/{id}")
    public MonthlyObjectiveResponse updateObjective(@PathVariable Long id, @RequestBody MonthlyObjectiveRequest request) {
        return itemService.updateObjective(id, request);
    }

    @PutMapping("/objectives/{id}/evaluation")
    public MonthlyObjectiveResponse evaluateObjective(@PathVariable Long id, @RequestBody EvaluationRequest request) {
        return itemService.evaluateObjective(id, request);
    }

    @DeleteMapping("/objectives/{id}")
    public ResponseEntity<Void> deleteObjective(@PathVariable Long id) {
        itemService.deleteObjective(id);
        return ResponseEntity.noContent().build();
    }
}
