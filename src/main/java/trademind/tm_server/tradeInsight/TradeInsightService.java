package trademind.tm_server.tradeInsight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.tradeInsight.dto.NewTradeInsightRequest;
import trademind.tm_server.tradeInsight.dto.TradeInsightResponse;
import trademind.tm_server.tradeInsight.repository.TradeInsightRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeInsightService {

    @Autowired
    private TradeInsightRepository repository;

    @Autowired
    private TradeInsightMapper mapper;

    private final String tableName = "Trade Insight";

    public List<TradeInsightResponse> create(List<NewTradeInsightRequest> requests) {
        List<TradeInsight> entities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<TradeInsight> saved = repository.saveAll(entities);

        return saved.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TradeInsightResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TradeInsightResponse> getByTradeId(Long tradeId) {
        return repository.findByTradeIdAndDeletedAtIsNull(tradeId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TradeInsightResponse getOneById(Long id) {
        TradeInsight entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(entity);
    }

    public void delete(Long id) {
        TradeInsight entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }
}
