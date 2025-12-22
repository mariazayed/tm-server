package trademind.tm_server.tradeExecution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeExecution.dto.NewTradeExecutionRequest;
import trademind.tm_server.tradeExecution.dto.TradeExecutionResponse;
import trademind.tm_server.tradeExecution.dto.UpdateTradeExecutionRequest;
import trademind.tm_server.tradeExecution.repository.TradeExecutionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeExecutionService {

    @Autowired
    private TradeExecutionRepository repository;

    @Autowired
    private TradeExecutionMapper mapper;

    private final String tableName = "Trade Execution";

    public TradeExecutionResponse create(NewTradeExecutionRequest trade) {
        TradeExecution tradePlanEntity = mapper.toEntity(trade);
        TradeExecution savedTradeExecution = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeExecution);
    }

    public List<TradeExecutionResponse> getAll() {
        List<TradeExecution> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeExecutionResponse
                .toList();
    }

    public TradeExecutionResponse getOneById(Long id) {
        TradeExecution tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public TradeExecution getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradeExecutionResponse update(Long id, UpdateTradeExecutionRequest tradeDetails) {
        TradeExecution tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        TradeExecution tradePlanEntity = mapper.toEntity(tradeDetails);
        TradeExecution savedTradeExecution = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeExecution);
    }

    public TradeExecutionResponse undoDelete(Long id) {
        TradeExecution tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        TradeExecution savedTradeExecution = repository.save(tradePlan);
        return mapper.toResponse(savedTradeExecution);
    }

    public void delete(Long id) {
        TradeExecution tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}