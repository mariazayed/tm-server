package trademind.tm_server.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.strategy.dto.NewStrategyRequest;
import trademind.tm_server.strategy.dto.StrategyResponse;
import trademind.tm_server.strategy.dto.StrategySearchRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_STRATEGIES)
public class StrategyController {
    @Autowired
    private StrategyService service;

    @PostMapping
    public List<StrategyResponse> create(@RequestBody List<NewStrategyRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<StrategyResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public StrategyResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<StrategyResponse> search(@RequestBody StrategySearchRequest request) {
        return service.search(request);
    }
}