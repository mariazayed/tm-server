package trademind.tm_server.tradeStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeStep.dto.NewTradeStepRequest;
import trademind.tm_server.tradeStep.dto.TradeStepResponse;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_TRADE_STEP)
public class TradeStepController {
    @Autowired
    private TradeStepService service;

    @PostMapping
    public List<TradeStepResponse> create(@RequestBody List<NewTradeStepRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<TradeStepResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_BY_TRADE_PARAM)
    public List<TradeStepResponse> getByTrade(@PathVariable Long tradeId) {
        return service.getByTradeId(tradeId);
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeStepResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
