package trademind.tm_server.tradePlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradePlan.dto.NewTradePlanRequest;
import trademind.tm_server.tradePlan.dto.TradePlanResponse;
import trademind.tm_server.tradePlan.dto.UpdateTradePlanRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_TRADE_PLAN)
public class TradePlanController {
    @Autowired
    private TradePlanService tradePlanService;

    @PostMapping
    public TradePlanResponse create(@RequestBody NewTradePlanRequest request) {
        return tradePlanService.create(request);
    }

    @GetMapping
    public List<TradePlanResponse> getAll() {
        return tradePlanService.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradePlanResponse getById(@PathVariable Long id) {
        return tradePlanService.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradePlanResponse update(@PathVariable Long id, @RequestBody UpdateTradePlanRequest request) {
        return tradePlanService.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tradePlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradePlanResponse undoDelete(@PathVariable Long id) {
        return tradePlanService.undoDelete(id);
    }
}