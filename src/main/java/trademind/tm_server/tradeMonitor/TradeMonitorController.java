package trademind.tm_server.tradeMonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeMonitor.dto.NewTradeMonitorRequest;
import trademind.tm_server.tradeMonitor.dto.TradeMonitorResponse;
import trademind.tm_server.tradeMonitor.dto.UpdateTradeMonitorRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE_MONITORING)
public class TradeMonitorController {
    @Autowired
    private TradeMonitorService service;

    @PostMapping
    public TradeMonitorResponse create(@RequestBody NewTradeMonitorRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TradeMonitorResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeMonitorResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeMonitorResponse update(@PathVariable Long id, @RequestBody UpdateTradeMonitorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradeMonitorResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }
}