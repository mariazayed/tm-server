package trademind.tm_server.tradeEvaluate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeEvaluate.dto.NewTradeEvaluateRequest;
import trademind.tm_server.tradeEvaluate.dto.TradeEvaluateResponse;
import trademind.tm_server.tradeEvaluate.dto.UpdateTradeEvaluateRequest;
import trademind.tm_server.tradeEvaluate.repository.TradeEvaluateRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeEvaluateService {

    @Autowired
    private TradeEvaluateRepository repository;

    @Autowired
    private TradeEvaluateMapper mapper;

    private final String tableName = "Trade Evaluation";

    public TradeEvaluateResponse create(NewTradeEvaluateRequest trade) {
        TradeEvaluate tradePlanEntity = mapper.toEntity(trade);
        TradeEvaluate savedTradeEvaluate = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeEvaluate);
    }

    public List<TradeEvaluateResponse> getAll() {
        List<TradeEvaluate> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeEvaluateResponse
                .toList();
    }

    public TradeEvaluateResponse getOneById(Long id) {
        TradeEvaluate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public TradeEvaluate getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradeEvaluateResponse update(Long id, UpdateTradeEvaluateRequest tradeDetails) {
        TradeEvaluate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        TradeEvaluate tradePlanEntity = mapper.toEntity(tradeDetails);
        TradeEvaluate savedTradeEvaluate = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeEvaluate);
    }

    public TradeEvaluateResponse undoDelete(Long id) {
        TradeEvaluate tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        TradeEvaluate savedTradeEvaluate = repository.save(tradePlan);
        return mapper.toResponse(savedTradeEvaluate);
    }

    public void delete(Long id) {
        TradeEvaluate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}