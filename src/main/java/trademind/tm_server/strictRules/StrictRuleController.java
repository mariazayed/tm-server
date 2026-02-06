package trademind.tm_server.strictRules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.strictRules.dto.NewStrictRuleRequest;
import trademind.tm_server.strictRules.dto.StrictRuleResponse;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_STRICT_RULES)
public class StrictRuleController {
    @Autowired
    private StrictRuleService service;

    @PostMapping
    public List<StrictRuleResponse> create(@RequestBody List<NewStrictRuleRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<StrictRuleResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public StrictRuleResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}