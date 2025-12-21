package trademind.tm_server.tradePlan;

import io.swagger.v3.oas.annotations.Parameter;
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
    public TradePlanResponse create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Asset object to be created with name and trading symbol",
                    required = true
            )
            @RequestBody NewTradePlanRequest plan) {
        return tradePlanService.create(plan);
    }

    @GetMapping
    public List<TradePlanResponse> getAllAssets() {
        return tradePlanService.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradePlanResponse getAssetById(
            @Parameter(description = "ID of the plan to retrieve", example = "1", required = true)
            @PathVariable Long id) {
        return tradePlanService.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradePlanResponse update(
            @Parameter(description = "ID of the plan to update", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody UpdateTradePlanRequest plan) {
        return tradePlanService.update(id, plan);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the plan to delete", example = "1", required = true)
            @PathVariable Long id) {
        tradePlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public TradePlanResponse undoDelete(
            @Parameter(description = "ID of the plan to restore", example = "1", required = true)
            @PathVariable Long id) {
        return tradePlanService.undoDelete(id);
    }
}