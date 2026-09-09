package trademind.tm_server.tradeStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeStep.dto.NewTradeStepRequest;
import trademind.tm_server.tradeStep.dto.TradeStepResponse;
import trademind.tm_server.tradeStep.repository.TradeStepRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeStepService {

    @Autowired
    private TradeStepRepository repository;

    @Autowired
    private TradeStepMapper mapper;

    private final String tableName = "Trade Step";

    public List<TradeStepResponse> create(List<NewTradeStepRequest> requests) {
        List<TradeStep> entities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<TradeStep> saved = repository.saveAll(entities);

        return saved.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TradeStepResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TradeStepResponse> getByTradeId(Long tradeId) {
        return repository.findByTradeIdAndDeletedAtIsNull(tradeId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TradeStepResponse getOneById(Long id) {
        TradeStep entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        TradeStep entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }
}
