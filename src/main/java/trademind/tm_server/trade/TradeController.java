package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.dto.TradeSearchRequest;
import trademind.tm_server.ai.AiSuggestionService;
import trademind.tm_server.trade.dto.TradeReviewRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_TRADE)
public class TradeController {
    @Autowired
    private TradeService service;

    @Autowired
    private AiSuggestionService aiSuggestionService;

    @PostMapping
    public List<TradeResponse> create(@RequestBody List<NewTradeRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<TradeResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    // Paginated fetch for the Trade List/Journal/Review screens -- see
    // TradeService.search().
    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<TradeResponse> search(@RequestBody TradeSearchRequest request) {
        return service.search(request);
    }

    @PatchMapping(AppConstants.SUB_URL_REVIEW)
    public TradeResponse review(@PathVariable Long id, @RequestBody TradeReviewRequest request) {
        return service.review(id, request);
    }

    // Phase 5: explicit, cost-gated AI-suggestion pass -- never called
    // automatically, only ever fills fields that are still null, and never
    // touches a trade that's already been reviewed. See AiSuggestionService.
    @PostMapping(AppConstants.SUB_URL_SUGGEST)
    public TradeResponse suggest(@PathVariable Long id) {
        return aiSuggestionService.suggest(id);
    }

    // Maintenance endpoint: re-applies TradeEnrichmentService to every
    // existing trade (see TradeService.recomputeEnrichment). Call this after
    // setting or correcting an asset's pipSize/contractSize so trades saved
    // before that change pick up the corrected figures too, instead of only
    // new trades going forward. Returns the number of trades touched.
    @PostMapping(AppConstants.SUB_URL_RECOMPUTE_ENRICHMENT)
    public int recomputeEnrichment() {
        return service.recomputeEnrichment();
    }
}