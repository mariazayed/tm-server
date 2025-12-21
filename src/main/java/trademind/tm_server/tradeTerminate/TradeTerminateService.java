package trademind.tm_server.tradeTerminate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeTerminate.dto.NewTradeTerminateRequest;
import trademind.tm_server.tradeTerminate.dto.TradeTerminateResponse;
import trademind.tm_server.tradeTerminate.dto.UpdateTradeTerminateRequest;
import trademind.tm_server.tradeTerminate.repository.TradeTerminateRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeTerminateService {

    @Autowired
    private TradeTerminateRepository repository;

    @Autowired
    private TradeTerminateMapper mapper;

    private final String tableName = "Trade Terminate";

    public TradeTerminateResponse create(NewTradeTerminateRequest trade) {
        TradeTerminate tradePlanEntity = mapper.toEntity(trade);
        TradeTerminate savedTradeTerminate = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeTerminate);
    }

    public List<TradeTerminateResponse> getAll() {
        List<TradeTerminate> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeTerminateResponse
                .toList();
    }

    public TradeTerminateResponse getOneById(Long id) {
        TradeTerminate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public TradeTerminate getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradeTerminateResponse update(Long id, UpdateTradeTerminateRequest tradeDetails) {
        TradeTerminate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        TradeTerminate tradePlanEntity = mapper.toEntity(tradeDetails);
        TradeTerminate savedTradeTerminate = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeTerminate);
    }

    public TradeTerminateResponse undoDelete(Long id) {
        TradeTerminate tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        TradeTerminate savedTradeTerminate = repository.save(tradePlan);
        return mapper.toResponse(savedTradeTerminate);
    }

    public void delete(Long id) {
        TradeTerminate tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}