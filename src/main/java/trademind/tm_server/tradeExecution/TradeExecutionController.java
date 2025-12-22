package trademind.tm_server.tradeExecution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeExecution.dto.NewTradeExecutionRequest;
import trademind.tm_server.tradeExecution.dto.TradeExecutionResponse;
import trademind.tm_server.tradeExecution.dto.UpdateTradeExecutionRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE_EXECUTION)
public class TradeExecutionController {
    @Autowired
    private TradeExecutionService service;

    @PostMapping
    public TradeExecutionResponse create(@RequestBody NewTradeExecutionRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TradeExecutionResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeExecutionResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeExecutionResponse update(@PathVariable Long id, @RequestBody UpdateTradeExecutionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradeExecutionResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }
}