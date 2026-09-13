package trademind.tm_server.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.strategy.dto.NewStrategyRequest;
import trademind.tm_server.strategy.dto.StrategyResponse;
import trademind.tm_server.strategy.dto.StrategySearchRequest;
import trademind.tm_server.strategy.repository.StrategyRepository;
import trademind.tm_server.strategy.repository.StrategySearchRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StrategyService {

    @Autowired
    private StrategyRepository repository;

    @Autowired
    private StrategySearchRepository searchRepository;

    @Autowired
    private StrategyMapper mapper;

    private final String tableName = "Strategy";

    public List<StrategyResponse> create(List<NewStrategyRequest> requests) {
        List<Strategy> strategyEntities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<Strategy> savedStrategies = repository.saveAll(strategyEntities);

        return savedStrategies.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<StrategyResponse> getAll() {
        List<Strategy> strategyPlanList = repository.findByDeletedAtIsNull();

        return strategyPlanList.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public StrategyResponse getOneById(Long id) {
        Strategy strategyPlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(strategyPlan);
    }

    public void delete(Long id) {
        Strategy object = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        object.setDeletedAt(LocalDateTime.now());
        repository.save(object);
    }

    // Real, server-side pagination, added mainly for consistency with the
    // other list screens (strategy counts are typically small).
    public SearchResponse<StrategyResponse> search(StrategySearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<Strategy> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}