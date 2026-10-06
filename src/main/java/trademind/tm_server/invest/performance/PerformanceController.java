package trademind.tm_server.invest.performance;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.invest.price.PriceService;

/** Under /api/v1/invest, so every route needs a valid login token. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST)
public class PerformanceController {

    private final PerformanceService performance;
    private final PriceService prices;

    public PerformanceController(PerformanceService performance, PriceService prices) {
        this.performance = performance;
        this.prices = prices;
    }

    @GetMapping("/performance")
    public PerformanceResponse performance(@RequestParam(defaultValue = "ALL") String range) {
        return performance.performance(range);
    }

    /** Pulls fresh closes now. A provider failure is reported in the body, not as an HTTP error. */
    @PostMapping("/prices/refresh")
    public PriceService.RefreshResult refresh() {
        return prices.refresh();
    }
}
