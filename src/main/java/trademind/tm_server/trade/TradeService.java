package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.trade.dto.InitTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.dto.UpdateTradeRequest;
import trademind.tm_server.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeService {

    @Autowired
    private TradeRepository repository;

    @Autowired
    private TradeMapper mapper;

    private final String tableName = "Trade";

    public TradeResponse initialize(InitTradeRequest request) {
        Trade tradePlanEntity = mapper.toEntity(request);
        Trade savedTrade = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTrade);
    }

    public List<TradeResponse> getAll() {
        List<Trade> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeResponse
                .toList();
    }

    public TradeResponse getOneById(Long id) {
        Trade tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public Trade getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradeResponse update(Long id, UpdateTradeRequest tradeDetails) {
        Trade tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        Trade tradePlanEntity = mapper.toEntity(tradeDetails);
        Trade savedTrade = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTrade);
    }

    public TradeResponse undoDelete(Long id) {
        Trade tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        Trade savedTrade = repository.save(tradePlan);
        return mapper.toResponse(savedTrade);
    }

    public void delete(Long id) {
        Trade tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}