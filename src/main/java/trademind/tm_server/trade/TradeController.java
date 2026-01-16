package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.dto.InitTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE)
public class TradeController {
    @Autowired
    private TradeService service;

    @PostMapping
    public TradeResponse initialize(@RequestBody InitTradeRequest request) {
        return service.initialize(request);
    }

    @GetMapping
    public List<TradeResponse> getAll() {
        return service.getAll();
    }

//    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
//    public TradeResponse getById(@PathVariable Long id) {
//        return service.getOneById(id);
//    }
//
//    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
//    public TradeResponse update(@PathVariable Long id, @RequestBody UpdateTradeRequest request) {
//        return service.update(id, request);
//    }
//
//    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        service.delete(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
//    public TradeResponse undoDelete(@PathVariable Long id) {
//        return service.undoDelete(id);
//    }
}