package trademind.tm_server.tradeEvaluate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeEvaluate.dto.NewTradeEvaluateRequest;
import trademind.tm_server.tradeEvaluate.dto.TradeEvaluateResponse;
import trademind.tm_server.tradeEvaluate.dto.UpdateTradeEvaluateRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE_EVALUATION)
public class TradeEvaluateController {
    @Autowired
    private TradeEvaluateService service;

    @PostMapping
    public TradeEvaluateResponse create(@RequestBody NewTradeEvaluateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TradeEvaluateResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeEvaluateResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeEvaluateResponse update(@PathVariable Long id, @RequestBody UpdateTradeEvaluateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradeEvaluateResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }
}