package trademind.tm_server.planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.enums.PlanItemStatusEnum;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.planner.dto.AreaAllocationResponse;
import trademind.tm_server.planner.dto.EvaluationSummaryResponse;
import trademind.tm_server.planner.dto.MonthPlanResponse;
import trademind.tm_server.planner.dto.MonthlyObjectiveResponse;
import trademind.tm_server.planner.dto.PlannerOverviewResponse;
import trademind.tm_server.planner.dto.PlannerSettingsDto;
import trademind.tm_server.planner.dto.WeekExtraRequest;
import trademind.tm_server.planner.dto.WeekExtrasRequest;
import trademind.tm_server.planner.dto.WeekPlanResponse;
import trademind.tm_server.planner.dto.WeekSnapshotResponse;
import trademind.tm_server.planner.repository.FocusAreaRepository;
import trademind.tm_server.planner.repository.MonthlyObjectiveRepository;
import trademind.tm_server.planner.repository.PlannerSettingsRepository;
import trademind.tm_server.planner.repository.PlannerWeekExtraRepository;
import trademind.tm_server.planner.repository.PlannerWeekRepository;
import trademind.tm_server.planner.repository.WeeklyTaskRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Planner capacity and summaries. Nothing here is stored except the
 * settings row, week notes and extra-hour entries -- capacity, per-area
 * allocation, evaluation stats and objective roll-ups are computed on every
 * read, so they always match the current tasks/objectives.
 *
 * Capacity rules:
 * - week  = sum of the seven weekday hours + all of that week's extra hours
 * - month = sum of weekday hours over every calendar day of the month
 *           + extra hours of weeks whose Monday falls inside the month
 *
 * Focus Area allowance (the max % budget):
 *   (weekday hours + GENERAL extra hours) x max% + extra hours DEDICATED to
 *   that area. So "+6h Backtesting" only raises Backtesting's budget, while
 *   "+6h general" raises every area's budget in proportion to its max %.
 */
@Service
public class PlannerService {

    @Autowired
    private PlannerSettingsRepository settingsRepository;

    @Autowired
    private PlannerWeekRepository weekRepository;

    @Autowired
    private PlannerWeekExtraRepository extraRepository;

    @Autowired
    private WeeklyTaskRepository taskRepository;

    @Autowired
    private MonthlyObjectiveRepository objectiveRepository;

    @Autowired
    private FocusAreaRepository focusAreaRepository;

    @Autowired
    private FocusAreaService focusAreaService;

    @Autowired
    private PlannerMapper mapper;

    // ---------------------------------------------------------------- settings

    public PlannerSettings getSettingsEntity() {
        return settingsRepository.findFirstByDeletedAtIsNullOrderByIdAsc()
                .orElseGet(() -> {
                    PlannerSettings defaults = new PlannerSettings();
                    defaults.setMondayHours(6);
                    defaults.setTuesdayHours(6);
                    defaults.setWednesdayHours(6);
                    defaults.setThursdayHours(6);
                    defaults.setFridayHours(6);
                    return settingsRepository.save(defaults);
                });
    }

    public PlannerSettingsDto getSettings() {
        return mapper.toResponse(getSettingsEntity());
    }

    public PlannerSettingsDto updateSettings(PlannerSettingsDto request) {
        PlannerSettings s = getSettingsEntity();
        if (request.getMondayHours() != null) s.setMondayHours(validDayHours(request.getMondayHours()));
        if (request.getTuesdayHours() != null) s.setTuesdayHours(validDayHours(request.getTuesdayHours()));
        if (request.getWednesdayHours() != null) s.setWednesdayHours(validDayHours(request.getWednesdayHours()));
        if (request.getThursdayHours() != null) s.setThursdayHours(validDayHours(request.getThursdayHours()));
        if (request.getFridayHours() != null) s.setFridayHours(validDayHours(request.getFridayHours()));
        if (request.getSaturdayHours() != null) s.setSaturdayHours(validDayHours(request.getSaturdayHours()));
        if (request.getSundayHours() != null) s.setSundayHours(validDayHours(request.getSundayHours()));
        return mapper.toResponse(settingsRepository.save(s));
    }

    private double validDayHours(double hours) {
        if (hours < 0 || hours > 24) throw new BadRequestException("Daily hours must be between 0 and 24");
        return hours;
    }

    // -------------------------------------------------------------- week notes

    @Transactional
    public WeekPlanResponse updateWeekNotes(WeekExtrasRequest request) {
        if (request.getWeekStart() == null) throw new BadRequestException("weekStart is required");
        LocalDate monday = toMonday(request.getWeekStart());

        PlannerWeek week = weekRepository.findByWeekStart(monday).orElseGet(() -> {
            PlannerWeek w = new PlannerWeek();
            w.setWeekStart(monday);
            return w;
        });
        if (request.getNotes() != null) week.setNotes(request.getNotes());
        week.setDeletedAt(null);
        weekRepository.save(week);

        return getWeekPlan(monday);
    }

    // ------------------------------------------------------------- extra hours

    @Transactional
    public WeekPlanResponse addExtra(WeekExtraRequest request) {
        if (request.getWeekStart() == null) throw new BadRequestException("weekStart is required");
        PlannerWeekExtra extra = new PlannerWeekExtra();
        extra.setWeekStart(toMonday(request.getWeekStart()));
        extra.setHours(validExtraHours(request.getHours()));
        if (request.getFocusAreaId() != null) {
            extra.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        }
        extra.setNotes(request.getNote());
        extraRepository.save(extra);
        return getWeekPlan(extra.getWeekStart());
    }

    @Transactional
    public WeekPlanResponse updateExtra(Long id, WeekExtraRequest request) {
        PlannerWeekExtra extra = getExtraEntity(id);
        if (request.getWeekStart() != null) extra.setWeekStart(toMonday(request.getWeekStart()));
        if (request.getHours() != null) extra.setHours(validExtraHours(request.getHours()));
        if (Boolean.TRUE.equals(request.getClearFocusArea())) {
            extra.setFocusArea(null);
        } else if (request.getFocusAreaId() != null) {
            extra.setFocusArea(focusAreaService.getOneByIdAsEntity(request.getFocusAreaId()));
        }
        if (request.getNote() != null) extra.setNotes(request.getNote());
        extraRepository.save(extra);
        return getWeekPlan(extra.getWeekStart());
    }

    @Transactional
    public WeekPlanResponse deleteExtra(Long id) {
        PlannerWeekExtra extra = getExtraEntity(id);
        extra.setDeletedAt(LocalDateTime.now());
        extraRepository.save(extra);
        return getWeekPlan(extra.getWeekStart());
    }

    private PlannerWeekExtra getExtraEntity(Long id) {
        return extraRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlannerWeekExtra", "id", id));
    }

    private double validExtraHours(Double hours) {
        if (hours == null || hours <= 0) throw new BadRequestException("Extra hours must be greater than 0");
        if (hours > 168) throw new BadRequestException("Extra hours cannot exceed a week");
        return hours;
    }

    /** General (untagged) extra hours vs hours dedicated to specific areas. */
    private record ExtraSplit(double general, Map<Long, Double> dedicated, Map<Long, FocusArea> dedicatedAreas) {
        double total() {
            return general + dedicated.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }

    private ExtraSplit split(Collection<PlannerWeekExtra> extras) {
        double general = 0;
        Map<Long, Double> dedicated = new HashMap<>();
        Map<Long, FocusArea> areas = new HashMap<>();
        for (PlannerWeekExtra e : extras) {
            if (e.getFocusArea() == null) {
                general += e.getHours();
            } else {
                dedicated.merge(e.getFocusArea().getId(), e.getHours(), Double::sum);
                areas.putIfAbsent(e.getFocusArea().getId(), e.getFocusArea());
            }
        }
        return new ExtraSplit(general, dedicated, areas);
    }

    // --------------------------------------------------------------- week plan

    @Transactional
    public WeekPlanResponse getWeekPlan(LocalDate anyDayOfWeek) {
        LocalDate monday = toMonday(anyDayOfWeek);
        LocalDate sunday = monday.plusDays(6);
        PlannerSettings settings = getSettingsEntity();
        PlannerWeek week = weekRepository.findByWeekStart(monday)
                .filter(w -> w.getDeletedAt() == null)
                .orElse(null);
        List<PlannerWeekExtra> extras = extraRepository.findByWeekStartAndDeletedAtIsNullOrderByIdAsc(monday);
        ExtraSplit split = split(extras);

        double base = settings.weeklyHours();
        double capacity = base + split.total();

        List<WeeklyTask> tasks = taskRepository.findByWeekStartAndDeletedAtIsNullOrderBySortOrderAscIdAsc(monday);
        double planned = sum(tasks, WeeklyTask::getEstimatedHours);

        WeekPlanResponse r = new WeekPlanResponse();
        r.setWeekStart(monday);
        r.setWeekEnd(sunday);
        r.setWorkingDays(settings.weeklyWorkingDays());
        r.setBaseHours(base);
        r.setExtraHours(split.total());
        r.setExtras(extras.stream().map(mapper::toResponse).toList());
        r.setNotes(week != null ? week.getNotes() : null);
        r.setCapacityHours(capacity);
        r.setPlannedHours(planned);
        r.setRemainingHours(capacity - planned);
        r.setOverCapacity(planned > capacity + 1e-9);

        r.setTasks(tasks.stream().map(mapper::toResponse).toList());
        r.setAreas(allocations(
                tasks,
                WeeklyTask::getFocusArea,
                WeeklyTask::getEstimatedHours,
                t -> t.getActualHours() != null ? t.getActualHours() : 0,
                capacity,
                base + split.general(),
                split));
        r.setEvaluation(evaluationSummary(
                tasks.stream().map(t -> new Evaluated(t.getStatus(), t.getScore(), t.getEstimatedHours(), t.getActualHours())).toList()));

        // Objectives of every month this week touches (a week can straddle two).
        Set<YearMonth> months = new LinkedHashSet<>();
        months.add(YearMonth.from(monday));
        months.add(YearMonth.from(sunday));
        List<MonthlyObjective> objectives = new ArrayList<>();
        for (YearMonth ym : months) {
            objectives.addAll(objectiveRepository
                    .findByPlanYearAndPlanMonthAndDeletedAtIsNullOrderBySortOrderAscIdAsc(ym.getYear(), ym.getMonthValue()));
        }
        r.setObjectives(toObjectiveResponses(objectives));

        return r;
    }

    // -------------------------------------------------------------- month plan

    @Transactional
    public MonthPlanResponse getMonthPlan(int year, int month) {
        if (month < 1 || month > 12) throw new BadRequestException("month must be 1-12");
        YearMonth ym = YearMonth.of(year, month);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        PlannerSettings settings = getSettingsEntity();

        double base = 0;
        int workingDays = 0;
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            double h = settings.hoursFor(d.getDayOfWeek());
            base += h;
            if (h > 0) workingDays++;
        }

        List<PlannerWeekExtra> monthExtras = extraRepository.findByWeekStartBetweenAndDeletedAtIsNull(first, last);
        ExtraSplit split = split(monthExtras);
        double capacity = base + split.total();

        List<MonthlyObjective> objectives = objectiveRepository
                .findByPlanYearAndPlanMonthAndDeletedAtIsNullOrderBySortOrderAscIdAsc(year, month);
        List<MonthlyObjectiveResponse> objectiveResponses = toObjectiveResponses(objectives);
        Map<Long, MonthlyObjectiveResponse> byId = objectiveResponses.stream()
                .collect(Collectors.toMap(MonthlyObjectiveResponse::getId, Function.identity()));

        double planned = sum(objectives, MonthlyObjective::getEstimatedHours);

        MonthPlanResponse r = new MonthPlanResponse();
        r.setYear(year);
        r.setMonth(month);
        r.setWorkingDays(workingDays);
        r.setBaseHours(base);
        r.setExtraHours(split.total());
        r.setCapacityHours(capacity);
        r.setPlannedHours(planned);
        r.setRemainingHours(capacity - planned);
        r.setOverCapacity(planned > capacity + 1e-9);
        r.setObjectives(objectiveResponses);

        // Actual hours of an objective: what the user entered at evaluation,
        // falling back to what its linked weekly tasks have credited so far.
        ToDoubleFunction<MonthlyObjective> actualOf = o -> o.getActualHours() != null
                ? o.getActualHours()
                : byId.get(o.getId()).getLinkedCreditedHours();

        r.setAreas(allocations(objectives, MonthlyObjective::getFocusArea,
                MonthlyObjective::getEstimatedHours, actualOf, capacity, base + split.general(), split));
        r.setEvaluation(evaluationSummary(objectives.stream()
                .map(o -> new Evaluated(o.getStatus(), o.getScore(), o.getEstimatedHours(), actualOf.applyAsDouble(o)))
                .toList()));

        // Week strip: every week whose Monday falls in this month.
        List<WeeklyTask> monthTasks = taskRepository.findByWeekStartBetweenAndDeletedAtIsNull(first, last);
        Map<LocalDate, List<WeeklyTask>> tasksByWeek = monthTasks.stream()
                .collect(Collectors.groupingBy(WeeklyTask::getWeekStart));
        Map<LocalDate, List<PlannerWeekExtra>> extrasByWeek = monthExtras.stream()
                .collect(Collectors.groupingBy(PlannerWeekExtra::getWeekStart));
        List<WeekSnapshotResponse> weeks = new ArrayList<>();
        for (LocalDate monday = first.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
             !monday.isAfter(last);
             monday = monday.plusWeeks(1)) {
            weeks.add(snapshot(monday, settings,
                    tasksByWeek.getOrDefault(monday, List.of()),
                    extrasByWeek.getOrDefault(monday, List.of())));
        }
        r.setWeeks(weeks);

        return r;
    }

    // ---------------------------------------------------------------- overview

    /** Everything the Home dashboard's Planner section shows, in one call. */
    @Transactional
    public PlannerOverviewResponse getOverview(int weeksBack) {
        int n = Math.max(1, Math.min(weeksBack, 26));
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = toMonday(today);
        LocalDate firstMonday = thisMonday.minusWeeks(n - 1L);
        PlannerSettings settings = getSettingsEntity();

        List<WeeklyTask> windowTasks = taskRepository.findByWeekStartBetweenAndDeletedAtIsNull(firstMonday, thisMonday);
        List<PlannerWeekExtra> windowExtras = extraRepository.findByWeekStartBetweenAndDeletedAtIsNull(firstMonday, thisMonday);
        Map<LocalDate, List<WeeklyTask>> tasksByWeek = windowTasks.stream()
                .collect(Collectors.groupingBy(WeeklyTask::getWeekStart));
        Map<LocalDate, List<PlannerWeekExtra>> extrasByWeek = windowExtras.stream()
                .collect(Collectors.groupingBy(PlannerWeekExtra::getWeekStart));

        List<WeekSnapshotResponse> history = new ArrayList<>();
        for (LocalDate m = firstMonday; !m.isAfter(thisMonday); m = m.plusWeeks(1)) {
            history.add(snapshot(m, settings,
                    tasksByWeek.getOrDefault(m, List.of()),
                    extrasByWeek.getOrDefault(m, List.of())));
        }

        PlannerOverviewResponse r = new PlannerOverviewResponse();
        r.setCurrentWeek(getWeekPlan(thisMonday));
        r.setCurrentMonth(getMonthPlan(today.getYear(), today.getMonthValue()));
        r.setHistory(history);
        r.setDaysLeftInWeek((int) ChronoUnit.DAYS.between(today, thisMonday.plusDays(7)));

        // Planning streak: consecutive weeks ending now with >= 1 task.
        int planning = 0;
        for (int i = history.size() - 1; i >= 0 && history.get(i).getTaskCount() > 0; i--) planning++;
        r.setPlanningStreak(planning);

        // Evaluation streak: consecutive finished weeks (ending last week)
        // with tasks, all of them evaluated.
        int evaluated = 0;
        for (int i = history.size() - 2; i >= 0; i--) {
            WeekSnapshotResponse w = history.get(i);
            if (w.getTaskCount() == 0 || w.getEvaluatedCount() < w.getTaskCount()) break;
            evaluated++;
        }
        r.setEvaluationStreak(evaluated);

        // Focus Area mix over the whole window.
        double windowBase = settings.weeklyHours() * history.size();
        ExtraSplit split = split(windowExtras);
        r.setAreaMix(allocations(
                windowTasks,
                WeeklyTask::getFocusArea,
                WeeklyTask::getEstimatedHours,
                t -> t.getActualHours() != null ? t.getActualHours() : 0,
                windowBase + split.total(),
                windowBase + split.general(),
                split));

        return r;
    }

    private WeekSnapshotResponse snapshot(LocalDate monday, PlannerSettings settings,
                                          List<WeeklyTask> tasks, List<PlannerWeekExtra> extras) {
        double extra = sum(extras, PlannerWeekExtra::getHours);
        WeekSnapshotResponse s = new WeekSnapshotResponse();
        s.setWeekStart(monday);
        s.setExtraHours(extra);
        s.setCapacityHours(settings.weeklyHours() + extra);
        s.setPlannedHours(sum(tasks, WeeklyTask::getEstimatedHours));
        s.setActualHours(sum(tasks, t -> t.getActualHours() != null ? t.getActualHours() : 0));
        s.setTaskCount(tasks.size());
        s.setDoneCount((int) tasks.stream().filter(t -> t.getStatus() == PlanItemStatusEnum.DONE).count());
        s.setEvaluatedCount((int) tasks.stream().filter(t -> t.getStatus() != PlanItemStatusEnum.PLANNED).count());
        s.setCompletionPct(completion(tasks.stream().map(WeeklyTask::getStatus).toList()));
        s.setAverageScore(tasks.stream()
                .map(WeeklyTask::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null));
        return s;
    }

    // ---------------------------------------------------------- carry over

    /**
     * Copies every unfinished task (not DONE) of the previous week into the
     * given week as a fresh, un-evaluated task. Tasks already present in the
     * target week (same title + focus area) are skipped, so clicking twice
     * doesn't duplicate anything.
     */
    @Transactional
    public WeekPlanResponse carryOverUnfinished(LocalDate anyDayOfWeek) {
        LocalDate monday = toMonday(anyDayOfWeek);
        LocalDate previous = monday.minusWeeks(1);
        LocalDate sunday = monday.plusDays(6);

        List<WeeklyTask> existing = taskRepository.findByWeekStartAndDeletedAtIsNullOrderBySortOrderAscIdAsc(monday);
        Set<String> existingKeys = existing.stream()
                .map(t -> key(t.getTitle(), t.getFocusArea()))
                .collect(Collectors.toSet());

        int nextOrder = existing.size();
        List<WeeklyTask> copies = new ArrayList<>();
        for (WeeklyTask t : taskRepository.findByWeekStartAndDeletedAtIsNullOrderBySortOrderAscIdAsc(previous)) {
            if (t.getStatus() == PlanItemStatusEnum.DONE) continue;
            if (existingKeys.contains(key(t.getTitle(), t.getFocusArea()))) continue;

            WeeklyTask copy = new WeeklyTask();
            copy.setWeekStart(monday);
            copy.setTitle(t.getTitle());
            copy.setNotes(t.getNotes());
            copy.setFocusArea(t.getFocusArea());
            copy.setEstimatedHours(t.getEstimatedHours());
            copy.setStatus(PlanItemStatusEnum.PLANNED);
            copy.setSortOrder(nextOrder++);

            // Keep the objective link only if the objective belongs to a
            // month this week touches.
            MonthlyObjective o = t.getMonthlyObjective();
            if (o != null && o.getDeletedAt() == null) {
                YearMonth om = YearMonth.of(o.getPlanYear(), o.getPlanMonth());
                if (om.equals(YearMonth.from(monday)) || om.equals(YearMonth.from(sunday))) {
                    copy.setMonthlyObjective(o);
                }
            }
            copies.add(copy);
        }
        taskRepository.saveAll(copies);

        return getWeekPlan(monday);
    }

    private String key(String title, FocusArea area) {
        return (title == null ? "" : title.trim().toLowerCase()) + "|" + (area == null ? "" : area.getId());
    }

    // ------------------------------------------------------------- helpers

    public static LocalDate toMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Objective responses with their roll-up from linked weekly tasks. */
    List<MonthlyObjectiveResponse> toObjectiveResponses(List<MonthlyObjective> objectives) {
        if (objectives.isEmpty()) return List.of();

        Map<Long, List<WeeklyTask>> linked = taskRepository
                .findByMonthlyObjectiveIdInAndDeletedAtIsNull(objectives.stream().map(MonthlyObjective::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(t -> t.getMonthlyObjective().getId()));

        return objectives.stream().map(o -> {
            MonthlyObjectiveResponse r = mapper.toResponse(o);
            List<WeeklyTask> tasks = linked.getOrDefault(o.getId(), List.of());
            double credited = sum(tasks, PlannerService::creditedHours);
            int done = (int) tasks.stream().filter(t -> t.getStatus() == PlanItemStatusEnum.DONE).count();

            r.setLinkedTaskCount(tasks.size());
            r.setLinkedDoneCount(done);
            r.setLinkedPlannedHours(sum(tasks, WeeklyTask::getEstimatedHours));
            r.setLinkedCreditedHours(credited);

            double progress;
            if (o.getStatus() == PlanItemStatusEnum.DONE) {
                progress = 100;
            } else if (o.getEstimatedHours() > 0) {
                progress = credited / o.getEstimatedHours() * 100;
            } else {
                progress = completion(tasks.stream().map(WeeklyTask::getStatus).toList());
            }
            r.setProgressPct(Math.max(0, Math.min(100, progress)));
            return r;
        }).toList();
    }

    static double creditedHours(WeeklyTask t) {
        if (t.getActualHours() != null) return t.getActualHours();
        return switch (t.getStatus()) {
            case DONE -> t.getEstimatedHours();
            case PARTIAL -> t.getEstimatedHours() * 0.5;
            default -> 0;
        };
    }

    /**
     * @param capacity      total capacity of the period (for plannedPct)
     * @param allowanceBase weekday hours + general extra hours (what max %
     *                      applies to)
     * @param extras        dedicated extra hours per area, added on top
     */
    private <T> List<AreaAllocationResponse> allocations(
            List<T> items,
            Function<T, FocusArea> areaOf,
            ToDoubleFunction<T> plannedOf,
            ToDoubleFunction<T> actualOf,
            double capacity,
            double allowanceBase,
            ExtraSplit extras) {

        // Every active area (so empty ones still show their budget), plus any
        // archived/deleted area that items or dedicated extra hours in this
        // period still use.
        Map<Long, FocusArea> areas = new LinkedHashMap<>();
        focusAreaRepository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
                .filter(a -> !a.isArchived())
                .forEach(a -> areas.put(a.getId(), a));
        items.stream().map(areaOf).filter(Objects::nonNull).forEach(a -> areas.putIfAbsent(a.getId(), a));
        extras.dedicatedAreas().forEach(areas::putIfAbsent);

        List<AreaAllocationResponse> result = new ArrayList<>();
        for (FocusArea area : areas.values()) {
            List<T> inArea = items.stream()
                    .filter(i -> areaOf.apply(i) != null && Objects.equals(areaOf.apply(i).getId(), area.getId()))
                    .toList();
            double planned = sum(inArea, plannedOf);
            double actual = sum(inArea, actualOf);
            double max = area.getMaxPercent() != null ? area.getMaxPercent() : 100;
            double dedicated = extras.dedicated().getOrDefault(area.getId(), 0.0);
            double allowed = allowanceBase * max / 100.0 + dedicated;

            AreaAllocationResponse a = new AreaAllocationResponse();
            a.setFocusArea(mapper.toResponse(area));
            a.setItemCount(inArea.size());
            a.setPlannedHours(planned);
            a.setActualHours(actual);
            a.setPlannedPct(capacity > 0 ? planned / capacity * 100 : 0);
            a.setActualPct(capacity > 0 ? actual / capacity * 100 : 0);
            a.setMaxPercent(max);
            a.setExtraHours(dedicated);
            a.setAllowedHours(allowed);
            a.setOverLimit(planned > allowed + 1e-9);
            result.add(a);
        }
        return result;
    }

    private record Evaluated(PlanItemStatusEnum status, Integer score, double planned, Double actual) {
    }

    private EvaluationSummaryResponse evaluationSummary(List<Evaluated> items) {
        EvaluationSummaryResponse s = new EvaluationSummaryResponse();
        s.setTotalCount(items.size());
        s.setDoneCount(count(items, PlanItemStatusEnum.DONE));
        s.setPartialCount(count(items, PlanItemStatusEnum.PARTIAL));
        s.setMissedCount(count(items, PlanItemStatusEnum.MISSED));
        s.setPendingCount(count(items, PlanItemStatusEnum.PLANNED));
        s.setAverageScore(items.stream()
                .map(Evaluated::score)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null));
        s.setCompletionPct(completion(items.stream().map(Evaluated::status).toList()));
        s.setPlannedHours(items.stream().mapToDouble(Evaluated::planned).sum());
        s.setActualHours(items.stream().mapToDouble(e -> e.actual() != null ? e.actual() : 0).sum());
        return s;
    }

    private int count(List<Evaluated> items, PlanItemStatusEnum status) {
        return (int) items.stream().filter(i -> i.status() == status).count();
    }

    private static double completion(Collection<PlanItemStatusEnum> statuses) {
        if (statuses.isEmpty()) return 0;
        double score = 0;
        for (PlanItemStatusEnum s : statuses) {
            if (s == PlanItemStatusEnum.DONE) score += 1;
            else if (s == PlanItemStatusEnum.PARTIAL) score += 0.5;
        }
        return score / statuses.size() * 100;
    }

    private static <T> double sum(Collection<T> items, ToDoubleFunction<T> f) {
        return items.stream().mapToDouble(f).sum();
    }
}
