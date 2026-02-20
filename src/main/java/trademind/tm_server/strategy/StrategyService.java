package trademind.tm_server.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.strategy.dto.NewStrategyRequest;
import trademind.tm_server.strategy.dto.StrategyResponse;
import trademind.tm_server.strategy.repository.StrategyRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StrategyService {

    @Autowired
    private StrategyRepository repository;

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
}