package trademind.tm_server.tradeInsight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeInsight.dto.NewTradeInsightRequest;
import trademind.tm_server.tradeInsight.dto.TradeInsightResponse;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_TRADE_INSIGHT)
public class TradeInsightController {
    @Autowired
    private TradeInsightService service;

    @PostMapping
    public List<TradeInsightResponse> create(@RequestBody List<NewTradeInsightRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<TradeInsightResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_BY_TRADE_PARAM)
    public List<TradeInsightResponse> getByTrade(@PathVariable Long tradeId) {
        return service.getByTradeId(tradeId);
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeInsightResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
