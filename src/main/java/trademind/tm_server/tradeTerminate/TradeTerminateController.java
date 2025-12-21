package trademind.tm_server.tradeTerminate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeTerminate.dto.NewTradeTerminateRequest;
import trademind.tm_server.tradeTerminate.dto.TradeTerminateResponse;
import trademind.tm_server.tradeTerminate.dto.UpdateTradeTerminateRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE_TERMINATION)
public class TradeTerminateController {
    @Autowired
    private TradeTerminateService service;

    @PostMapping
    public TradeTerminateResponse create(@RequestBody NewTradeTerminateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TradeTerminateResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeTerminateResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeTerminateResponse update(@PathVariable Long id, @RequestBody UpdateTradeTerminateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradeTerminateResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }
}