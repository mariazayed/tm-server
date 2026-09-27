package trademind.tm_server.planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.enums.PlanItemStatusEnum;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.planner.dto.EvaluationRequest;
import trademind.tm_server.planner.dto.MonthlyObjectiveRequest;
import trademind.tm_server.planner.dto.MonthlyObjectiveResponse;
import trademind.tm_server.planner.dto.WeeklyTaskRequest;
import trademind.tm_server.planner.dto.WeeklyTaskResponse;
import trademind.tm_server.planner.repository.MonthlyObjectiveRepository;
import trademind.tm_server.planner.repository.WeeklyTaskRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// CRUD + evaluation for weekly tasks and monthly objectives. Over-limit
// allocations are deliberately NOT rejected here -- the per-area max % is a
// soft limit the UI warns about.
@Service
public class PlannerItemService {

    @Autowired
    private WeeklyTaskRepository taskRepository;

    @Autowired
    private MonthlyObjectiveRepository objectiveRepository;

    @Autowired
    private FocusAreaService focusAreaService;

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private PlannerMapper mapper;

    // ------------------------------------------------------------ weekly tasks

    @Transactional
    public WeeklyTaskResponse createTask(WeeklyTaskRequest request) {
        if (request.getWeekStart() == null) throw new BadRequestException("weekStart is required");
        requireTitle(request.getTitle());

        LocalDate monday = PlannerService.toMonday(request.getWeekStart());
        WeeklyTask task = new WeeklyTask();
        task.setWeekStart(monday);
        task.setTitle(request.getTitle().trim());
        task.setNotes(request.getNotes());
        task.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        task.setEstimatedHours(validHours(request.getEstimatedHours() != null ? request.getEstimatedHours() : 0));
        task.setStatus(PlanItemStatusEnum.PLANNED);
        task.setExtra(Boolean.TRUE.equals(request.getExtra()));
        if (task.isExtra() && task.getEstimatedHours() <= 0) {
            throw new BadRequestException("Extra hours must be greater than 0");
        }
        if (request.getActualHours() != null) task.setActualHours(validHours(request.getActualHours()));
        task.setSortOrder(request.getSortOrder() != null
                ? request.getSortOrder()
                : taskRepository.findByWeekStartAndDeletedAtIsNullOrderBySortOrderAscIdAsc(monday).size());
        if (request.getMonthlyObjectiveId() != null) {
            task.setMonthlyObjective(getObjectiveEntity(request.getMonthlyObjectiveId()));
        }

        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public WeeklyTaskResponse updateTask(Long id, WeeklyTaskRequest request) {
        WeeklyTask task = getTaskEntity(id);

        if (request.getWeekStart() != null) task.setWeekStart(PlannerService.toMonday(request.getWeekStart()));
        if (request.getTitle() != null) {
            requireTitle(request.getTitle());
            task.setTitle(request.getTitle().trim());
        }
        if (request.getFocusAreaId() != null) task.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        if (request.getEstimatedHours() != null) task.setEstimatedHours(validHours(request.getEstimatedHours()));
        // Actual time is independent of the estimate -- going over is normal
        // and is shown, never rejected.
        if (Boolean.TRUE.equals(request.getClearActualHours())) {
            task.setActualHours(null);
        } else if (request.getActualHours() != null) {
            task.setActualHours(validHours(request.getActualHours()));
        }
        if (request.getSortOrder() != null) task.setSortOrder(request.getSortOrder());
        if (request.getNotes() != null) task.setNotes(request.getNotes());
        if (Boolean.TRUE.equals(request.getUnlinkObjective())) {
            task.setMonthlyObjective(null);
        } else if (request.getMonthlyObjectiveId() != null) {
            task.setMonthlyObjective(getObjectiveEntity(request.getMonthlyObjectiveId()));
        }

        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public WeeklyTaskResponse evaluateTask(Long id, EvaluationRequest request) {
        WeeklyTask task = getTaskEntity(id);
        task.setStatus(request.getStatus() != null ? request.getStatus() : PlanItemStatusEnum.PLANNED);
        task.setScore(validScore(request.getScore()));
        task.setActualHours(request.getActualHours() != null ? validHours(request.getActualHours()) : null);
        task.setEvaluationNotes(request.getEvaluationNotes());
        return mapper.toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        WeeklyTask task = getTaskEntity(id);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private WeeklyTask getTaskEntity(Long id) {
        return taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklyTask", "id", id));
    }

    // ------------------------------------------------------ monthly objectives

    @Transactional
    public MonthlyObjectiveResponse createObjective(MonthlyObjectiveRequest request) {
        if (request.getPlanYear() == null || request.getPlanMonth() == null) {
            throw new BadRequestException("planYear and planMonth are required");
        }
        requireTitle(request.getTitle());

        MonthlyObjective o = new MonthlyObjective();
        o.setPlanYear(request.getPlanYear());
        o.setPlanMonth(validMonth(request.getPlanMonth()));
        o.setTitle(request.getTitle().trim());
        o.setNotes(request.getNotes());
        o.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        o.setEstimatedHours(validHours(request.getEstimatedHours() != null ? request.getEstimatedHours() : 0));
        o.setStatus(PlanItemStatusEnum.PLANNED);
        o.setSortOrder(request.getSortOrder() != null
                ? request.getSortOrder()
                : objectiveRepository.findByPlanYearAndPlanMonthAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
                        request.getPlanYear(), request.getPlanMonth()).size());

        return toResponse(objectiveRepository.save(o));
    }

    @Transactional
    public MonthlyObjectiveResponse updateObjective(Long id, MonthlyObjectiveRequest request) {
        MonthlyObjective o = getObjectiveEntity(id);

        if (request.getPlanYear() != null) o.setPlanYear(request.getPlanYear());
        if (request.getPlanMonth() != null) o.setPlanMonth(validMonth(request.getPlanMonth()));
        if (request.getTitle() != null) {
            requireTitle(request.getTitle());
            o.setTitle(request.getTitle().trim());
        }
        if (request.getFocusAreaId() != null) o.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        if (request.getEstimatedHours() != null) o.setEstimatedHours(validHours(request.getEstimatedHours()));
        if (request.getSortOrder() != null) o.setSortOrder(request.getSortOrder());
        if (request.getNotes() != null) o.setNotes(request.getNotes());

        return toResponse(objectiveRepository.save(o));
    }

    @Transactional
    public MonthlyObjectiveResponse evaluateObjective(Long id, EvaluationRequest request) {
        MonthlyObjective o = getObjectiveEntity(id);
        o.setStatus(request.getStatus() != null ? request.getStatus() : PlanItemStatusEnum.PLANNED);
        o.setScore(validScore(request.getScore()));
        o.setActualHours(request.getActualHours() != null ? validHours(request.getActualHours()) : null);
        o.setEvaluationNotes(request.getEvaluationNotes());
        return toResponse(objectiveRepository.save(o));
    }

    // Soft delete; linked weekly tasks are unlinked so they don't point at a
    // hidden objective.
    @Transactional
    public void deleteObjective(Long id) {
        MonthlyObjective o = getObjectiveEntity(id);
        List<WeeklyTask> linked = taskRepository.findByMonthlyObjectiveIdAndDeletedAtIsNull(id);
        linked.forEach(t -> t.setMonthlyObjective(null));
        taskRepository.saveAll(linked);

        o.setDeletedAt(LocalDateTime.now());
        objectiveRepository.save(o);
    }

    private MonthlyObjective getObjectiveEntity(Long id) {
        return objectiveRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonthlyObjective", "id", id));
    }

    private MonthlyObjectiveResponse toResponse(MonthlyObjective o) {
        return plannerService.toObjectiveResponses(List.of(o)).get(0);
    }

    // ------------------------------------------------------------- validation

    private void requireTitle(String title) {
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required");
    }

    private double validHours(double hours) {
        if (hours < 0) throw new BadRequestException("Hours cannot be negative");
        return hours;
    }

    private Integer validScore(Integer score) {
        if (score != null && (score < 1 || score > 5)) throw new BadRequestException("Score must be between 1 and 5");
        return score;
    }

    private int validMonth(int month) {
        if (month < 1 || month > 12) throw new BadRequestException("planMonth must be 1-12");
        return month;
    }
}
