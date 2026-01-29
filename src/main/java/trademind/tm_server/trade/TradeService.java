package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.asset.AssetLookupMapper;
import trademind.tm_server.capital.CapitalService;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.repository.TradeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TradeService {

    @Autowired
    private TradeRepository repository;

    @Autowired
    private CapitalService capitalService;

    @Autowired
    private TradeMapper mapper;

    private final String tableName = "Trade";

    public List<TradeResponse> create(List<NewTradeRequest> requests) {
        List<NewCapitalRequest> capitals = new ArrayList<>();

        for (NewTradeRequest request : requests) {
            NewCapitalRequest capital = new NewCapitalRequest();
            capital.setAmount(request.getPnl());
            capital.setDateTime(request.getExitDateTime());
            capital.setReason(request.getPnl() > 0 ? CapitalReasonEnum.PROFIT : CapitalReasonEnum.LOSS);
            capitals.add(capital);
        }

        capitalService.create(capitals);

        List<Trade> tradeEntities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<Trade> savedTrades = repository.saveAll(tradeEntities);

        return savedTrades.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TradeResponse> getAll() {
        List<Trade> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TradeResponse getOneById(Long id) {
        Trade tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }
}