package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.capital.CapitalService;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.enums.TradeSourceEnum;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.dto.TradeReviewRequest;
import trademind.tm_server.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TradeService {

    @Autowired
    private TradeRepository repository;

    @Autowired
    private CapitalService capitalService;

    @Autowired
    private TradeMapper mapper;

    @Autowired
    private TradeEnrichmentService enrichmentService;

    private final String tableName = "Trade";

    public List<TradeResponse> create(List<NewTradeRequest> requests) {
        // Dedupe against externalId (broker-side identifier) in one batch query,
        // before anything else gets created for these requests -- otherwise a
        // re-imported overlapping date range would double-book Capital rows even
        // if the duplicate Trade insert itself were skipped later.
        List<String> incomingExternalIds = requests.stream()
                .map(NewTradeRequest::getExternalId)
                .filter(Objects::nonNull)
                .toList();

        Set<String> existingExternalIds = incomingExternalIds.isEmpty()
                ? Set.of()
                : repository.findByExternalIdIn(incomingExternalIds).stream()
                    .map(Trade::getExternalId)
                    .collect(Collectors.toSet());

        List<NewTradeRequest> newRequests = filterNewRequests(requests, existingExternalIds);

        if (newRequests.isEmpty()) {
            return List.of();
        }

        List<NewCapitalRequest> capitals = new ArrayList<>();

        for (NewTradeRequest request : newRequests) {
            NewCapitalRequest capital = new NewCapitalRequest();
            capital.setAmount(request.getPnl());
            capital.setDateTime(request.getExitDateTime());
            capital.setReason(request.getPnl() > 0 ? CapitalReasonEnum.PROFIT : CapitalReasonEnum.LOSS);
            capitals.add(capital);
        }

        capitalService.create(capitals);

        List<Trade> tradeEntities = newRequests.stream()
                .map(mapper::toEntity)
                .toList();

        tradeEntities.forEach(trade -> {
            // Manual entries from the app won't set a source; imported ones will.
            if (trade.getSource() == null) {
                trade.setSource(TradeSourceEnum.MANUAL);
            }
            // Always recomputed server-side -- one source of truth regardless
            // of what the caller sent for session/dayOfWeek/tradeType/
            // riskPercent/plannedRiskReward/rMultiple.
            enrichmentService.enrich(trade);
        });

        List<Trade> savedTrades = repository.saveAll(tradeEntities);

        return savedTrades.stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Pure filter, extracted for direct unit testing (see TradeServiceTest)
    // without a Spring context or a real repository: a request survives if it
    // either has no externalId (manual entry) or its externalId isn't among
    // the ones the batch dedupe query already found in the DB. This is what
    // makes re-running an overlapping importer date range safe -- the second
    // run's duplicate positions are dropped here before anything else (Capital
    // rows included) gets created for them.
    static List<NewTradeRequest> filterNewRequests(List<NewTradeRequest> requests, Set<String> existingExternalIds) {
        return requests.stream()
                .filter(request -> request.getExternalId() == null
                        || !existingExternalIds.contains(request.getExternalId()))
                .toList();
    }

    public List<TradeResponse> getAll() {
        List<Trade> data = repository.findByDeletedAtIsNull();

        return data.stream()
                .sorted(Comparator.comparing(Trade::getCreatedAt).reversed())
                .map(mapper::toResponse)
                .toList();
    }

    public TradeResponse getOneById(Long id) {
        Trade tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    // Partial update for the review flow: only the AI-suggested/manual fields a
    // person confirms or fills in, same null-means-unchanged convention as
    // AssetService.update(). Marks the trade reviewed regardless of how many
    // fields were actually changed -- confirming "looks right, nothing to add"
    // is still a completed review.
    public TradeResponse review(Long id, TradeReviewRequest request) {
        Trade trade = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (request.getEntryReason() != null) trade.setEntryReason(request.getEntryReason());
        if (request.getEntryConfirmation() != null) trade.setEntryConfirmation(request.getEntryConfirmation());
        if (request.getEntryZoneType() != null) trade.setEntryZoneType(request.getEntryZoneType());
        if (request.getMarketBias() != null) trade.setMarketBias(request.getMarketBias());
        if (request.getMarketState() != null) trade.setMarketState(request.getMarketState());
        if (request.getInvalidationCondition() != null) trade.setInvalidationCondition(request.getInvalidationCondition());
        if (request.getSlPlacementReason() != null) trade.setSlPlacementReason(request.getSlPlacementReason());
        if (request.getTpPlacementReason() != null) trade.setTpPlacementReason(request.getTpPlacementReason());
        if (request.getExitReason() != null) trade.setExitReason(request.getExitReason());
        if (request.getProbability() != null) trade.setProbability(request.getProbability());
        if (request.getFollowedPlan() != null) trade.setFollowedPlan(request.getFollowedPlan());
        if (request.getDisciplineScore() != null) trade.setDisciplineScore(request.getDisciplineScore());
        if (request.getMistakeTags() != null) trade.setMistakeTags(request.getMistakeTags());

        trade.setReviewedAt(LocalDateTime.now());

        Trade saved = repository.save(trade);
        return mapper.toResponse(saved);
    }
}
