package trademind.tm_server.decision;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.decision.dto.DecisionResponse;
import trademind.tm_server.decision.dto.DecisionSearchRequest;
import trademind.tm_server.decision.dto.NewDecisionRequest;
import trademind.tm_server.decision.dto.UpdateDecisionRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_DECISION)
public class DecisionController {
    @Autowired
    private DecisionService service;

    @PostMapping
    public List<DecisionResponse> create(@RequestBody List<NewDecisionRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<DecisionResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public DecisionResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public DecisionResponse update(@PathVariable Long id, @RequestBody UpdateDecisionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<DecisionResponse> search(@RequestBody DecisionSearchRequest request) {
        return service.search(request);
    }
}
