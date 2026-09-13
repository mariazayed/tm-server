package trademind.tm_server.decision;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.decision.dto.DecisionResponse;
import trademind.tm_server.decision.dto.DecisionSearchRequest;
import trademind.tm_server.decision.dto.NewDecisionRequest;
import trademind.tm_server.decision.dto.UpdateDecisionRequest;
import trademind.tm_server.decision.repository.DecisionRepository;
import trademind.tm_server.decision.repository.DecisionSearchRepository;
import trademind.tm_server.enums.DecisionStatusEnum;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DecisionService {

    @Autowired
    private DecisionRepository repository;

    @Autowired
    private DecisionSearchRepository searchRepository;

    @Autowired
    private DecisionMapper mapper;

    private final String tableName = "Decision";

    public List<DecisionResponse> create(List<NewDecisionRequest> requests) {
        // decisionNumber is always server-assigned, never taken from the
        // client (NewDecisionRequest has no such field) -- continues the
        // log's 1, 2, 3... sequence. Assigned sequentially within this
        // batch too, so submitting several at once doesn't collide.
        int nextNumber = repository.findMaxDecisionNumber() + 1;

        List<Decision> entities = new ArrayList<>();
        for (NewDecisionRequest request : requests) {
            Decision decision = mapper.toEntity(request);
            decision.setStatus(DecisionStatusEnum.ACTIVE);
            decision.setDecisionNumber(nextNumber++);
            entities.add(decision);
        }

        List<Decision> saved = repository.saveAll(entities);

        return saved.stream().map(mapper::toResponse).toList();
    }

    public List<DecisionResponse> getAll() {
        List<Decision> decisions = repository.findByDeletedAtIsNull();

        return decisions.stream()
                .sorted(Comparator.comparing(Decision::getDecisionDate).reversed())
                .map(mapper::toResponse)
                .toList();
    }

    public DecisionResponse getOneById(Long id) {
        Decision decision = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(decision);
    }

    public DecisionResponse update(Long id, UpdateDecisionRequest request) {
        Decision entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        mapper.updateEntity(request, entity);

        return mapper.toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        Decision decision = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        decision.setDeletedAt(LocalDateTime.now());
        repository.save(decision);
    }

    // Real, server-side pagination, added for consistency with the other
    // list screens.
    public SearchResponse<DecisionResponse> search(DecisionSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<Decision> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}
