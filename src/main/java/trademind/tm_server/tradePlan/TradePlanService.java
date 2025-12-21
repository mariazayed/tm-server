package trademind.tm_server.tradePlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradePlan.dto.NewTradePlanRequest;
import trademind.tm_server.tradePlan.dto.TradePlanResponse;
import trademind.tm_server.tradePlan.dto.UpdateTradePlanRequest;
import trademind.tm_server.tradePlan.repository.TradePlanRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradePlanService {

    @Autowired
    private TradePlanRepository repository;

    @Autowired
    private TradePlanMapper mapper;

    private final String tableName = "Trade Plan";

    public TradePlanResponse create(NewTradePlanRequest trade) {
        TradePlan tradePlanEntity = mapper.toEntity(trade);
        TradePlan savedTradePlan = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradePlan);
    }

    public List<TradePlanResponse> getAll() {
        List<TradePlan> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradePlanResponse
                .toList();
    }

    public TradePlanResponse getOneById(Long id) {
        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public TradePlan getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradePlanResponse update(Long id, UpdateTradePlanRequest tradeDetails) {
        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        TradePlan tradePlanEntity = mapper.toEntity(tradeDetails);
        TradePlan savedTradePlan = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradePlan);
    }

    public TradePlanResponse undoDelete(Long id) {
        TradePlan tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        TradePlan savedTradePlan = repository.save(tradePlan);
        return mapper.toResponse(savedTradePlan);
    }

    public void delete(Long id) {
        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}