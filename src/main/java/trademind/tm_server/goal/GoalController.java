package trademind.tm_server.goal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.goal.dto.GoalResponse;
import trademind.tm_server.goal.dto.GoalSearchRequest;
import trademind.tm_server.goal.dto.NewGoalRequest;
import trademind.tm_server.goal.dto.UpdateGoalRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_GOAL)
public class GoalController {
    @Autowired
    private GoalService service;

    @PostMapping
    public List<GoalResponse> create(@RequestBody List<NewGoalRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<GoalResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public GoalResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public GoalResponse update(@PathVariable Long id, @RequestBody UpdateGoalRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<GoalResponse> search(@RequestBody GoalSearchRequest request) {
        return service.search(request);
    }
}
