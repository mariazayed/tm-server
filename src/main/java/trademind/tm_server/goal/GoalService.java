package trademind.tm_server.goal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.capital.Capital;
import trademind.tm_server.capital.repository.CapitalRepository;
import trademind.tm_server.day.Day;
import trademind.tm_server.day.repository.DayRepository;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.enums.GoalStatusEnum;
import trademind.tm_server.enums.ScoreEnum;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.goal.dto.GoalResponse;
import trademind.tm_server.goal.dto.GoalSearchRequest;
import trademind.tm_server.goal.dto.NewGoalRequest;
import trademind.tm_server.goal.dto.UpdateGoalRequest;
import trademind.tm_server.goal.repository.GoalRepository;
import trademind.tm_server.goal.repository.GoalSearchRepository;
import trademind.tm_server.trade.Trade;
import trademind.tm_server.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Goal progress is never stored - it's computed fresh on every read from the
 * existing Trade/Capital/Day data, so it's always in sync with the journal.
 * Only MANUAL goals carry a hand-entered progress value.
 */
@Service
public class GoalService {

    @Autowired
    private GoalRepository repository;

    @Autowired
    private GoalSearchRepository searchRepository;

    @Autowired
    private GoalMapper mapper;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private CapitalRepository capitalRepository;

    @Autowired
    private DayRepository dayRepository;

    // A day counts towards a discipline goal when its psychology rating is
    // GOOD or better.
    private static final int DISCIPLINE_THRESHOLD_INDEX = ScoreEnum.GOOD.getIndex();

    private final String tableName = "Goal";

    public List<GoalResponse> create(List<NewGoalRequest> requests) {
        List<Goal> entities = requests.stream().map(request -> {
            Goal goal = mapper.toEntity(request);
            goal.setStatus(GoalStatusEnum.ACTIVE);
            return goal;
        }).toList();

        List<Goal> saved = repository.saveAll(entities);

        return saved.stream().map(this::toComputedResponse).toList();
    }

    public List<GoalResponse> getAll() {
        List<Goal> goals = repository.findByDeletedAtIsNull();

        return goals.stream()
                .sorted(Comparator.comparing(Goal::getCreatedAt).reversed())
                .map(this::toComputedResponse)
                .toList();
    }

    public GoalResponse getOneById(Long id) {
        Goal goal = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return toComputedResponse(goal);
    }

    public GoalResponse update(Long id, UpdateGoalRequest request) {
        Goal entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        mapper.updateEntity(request, entity);

        return toComputedResponse(repository.save(entity));
    }

    public void delete(Long id) {
        Goal goal = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        goal.setDeletedAt(LocalDateTime.now());
        repository.save(goal);
    }

    // Real, server-side pagination, added mainly for consistency with the
    // other list screens (goal counts are typically small). Routed through
    // toComputedResponse so paginated results still carry the computed
    // progress fields, same as getAll().
    public SearchResponse<GoalResponse> search(GoalSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<Goal> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, this::toComputedResponse);
    }

    private GoalResponse toComputedResponse(Goal goal) {
        GoalResponse response = mapper.toResponse(goal);

        double currentValue = computeCurrentValue(goal);
        double progressPct = computeProgressPct(goal, currentValue);

        response.setCurrentValue(currentValue);
        response.setProgressPct(progressPct);
        response.setAchieved(progressPct >= 100.0);
        response.setDaysRemaining(computeDaysRemaining(goal));

        return response;
    }

    private double computeCurrentValue(Goal goal) {
        switch (goal.getCategory()) {
            case PROFIT:
                return netProfitBetween(goal.getStartDate(), goal.getEndDate());
            case TRADE_QUALITY:
                return winRateBetween(goal.getStartDate(), goal.getEndDate());
            case DISCIPLINE:
                return disciplineRateBetween(goal.getStartDate(), goal.getEndDate());
            case MANUAL:
            default:
                return goal.getManualProgressPct() != null ? goal.getManualProgressPct() : 0.0;
        }
    }

    private double computeProgressPct(Goal goal, double currentValue) {
        switch (goal.getCategory()) {
            case PROFIT:
            case TRADE_QUALITY:
                if (goal.getTargetValue() == null || goal.getTargetValue() <= 0) return 0.0;
                return clamp((currentValue / goal.getTargetValue()) * 100.0);
            case DISCIPLINE:
                double target = goal.getTargetValue() != null && goal.getTargetValue() > 0
                        ? goal.getTargetValue()
                        : 100.0;
                return clamp((currentValue / target) * 100.0);
            case MANUAL:
            default:
                return clamp(currentValue);
        }
    }

    private double clamp(double value) {
        if (value < 0) return 0.0;
        if (value > 100) return 100.0;
        return value;
    }

    private long computeDaysRemaining(Goal goal) {
        LocalDateTime now = LocalDateTime.now();
        if (goal.getEndDate() == null || now.isAfter(goal.getEndDate())) return 0;
        return ChronoUnit.DAYS.between(now.toLocalDate(), goal.getEndDate().toLocalDate());
    }

    private double netProfitBetween(LocalDateTime start, LocalDateTime end) {
        return capitalRepository.findByDeletedAtIsNull().stream()
                .filter(c -> c.getReason() == CapitalReasonEnum.PROFIT || c.getReason() == CapitalReasonEnum.LOSS)
                .filter(c -> isBetween(c.getDateTime(), start, end))
                .mapToDouble(Capital::getAmount)
                .sum();
    }

    private double winRateBetween(LocalDateTime start, LocalDateTime end) {
        List<Trade> trades = tradeRepository.findByDeletedAtIsNull().stream()
                .filter(t -> isBetween(t.getExitDateTime(), start, end))
                .toList();

        if (trades.isEmpty()) return 0.0;

        long wins = trades.stream().filter(t -> t.getPnl() > 0).count();

        return (wins * 100.0) / trades.size();
    }

    private double disciplineRateBetween(LocalDateTime start, LocalDateTime end) {
        List<Day> days = dayRepository.findByDeletedAtIsNull().stream()
                .filter(d -> isBetween(d.getDate(), start, end))
                .filter(d -> !d.isWeekend() && !d.isNoWork())
                .toList();

        if (days.isEmpty()) return 0.0;

        long goodDays = days.stream()
                .filter(d -> d.getPsychologyRate() != null
                        && d.getPsychologyRate().getIndex() >= DISCIPLINE_THRESHOLD_INDEX)
                .count();

        return (goodDays * 100.0) / days.size();
    }

    private boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) return false;
        return !value.isBefore(start) && !value.isAfter(end);
    }
}
