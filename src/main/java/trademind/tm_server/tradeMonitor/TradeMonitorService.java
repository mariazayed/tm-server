package trademind.tm_server.tradeMonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeMonitor.dto.NewTradeMonitorRequest;
import trademind.tm_server.tradeMonitor.dto.TradeMonitorResponse;
import trademind.tm_server.tradeMonitor.dto.UpdateTradeMonitorRequest;
import trademind.tm_server.tradeMonitor.repository.TradeMonitorRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeMonitorService {

    @Autowired
    private TradeMonitorRepository repository;

    @Autowired
    private TradeMonitorMapper mapper;

    private final String tableName = "Trade Monitoring";

    public TradeMonitorResponse create(NewTradeMonitorRequest trade) {
        TradeMonitor tradePlanEntity = mapper.toEntity(trade);
        TradeMonitor savedTradeMonitor = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeMonitor);
    }

    public List<TradeMonitorResponse> getAll() {
        List<TradeMonitor> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeMonitorResponse
                .toList();
    }

    public TradeMonitorResponse getOneById(Long id) {
        TradeMonitor tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public TradeMonitor getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public TradeMonitorResponse update(Long id, UpdateTradeMonitorRequest tradeDetails) {
        TradeMonitor tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        TradeMonitor tradePlanEntity = mapper.toEntity(tradeDetails);
        TradeMonitor savedTradeMonitor = repository.save(tradePlanEntity);
        return mapper.toResponse(savedTradeMonitor);
    }

    public TradeMonitorResponse undoDelete(Long id) {
        TradeMonitor tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        TradeMonitor savedTradeMonitor = repository.save(tradePlan);
        return mapper.toResponse(savedTradeMonitor);
    }

    public void delete(Long id) {
        TradeMonitor tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}