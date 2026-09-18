package trademind.tm_server.expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.expense.dto.ExpenseResponse;
import trademind.tm_server.expense.dto.ExpenseSearchRequest;
import trademind.tm_server.expense.dto.NewExpenseRequest;
import trademind.tm_server.expense.dto.UpdateExpenseRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_EXPENSE)
public class ExpenseController {
    @Autowired
    private ExpenseService service;

    @PostMapping
    public ExpenseResponse create(@RequestBody NewExpenseRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ExpenseResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public ExpenseResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public ExpenseResponse update(
            @PathVariable Long id,
            @RequestBody UpdateExpenseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public ExpenseResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<ExpenseResponse> search(@RequestBody ExpenseSearchRequest request) {
        return service.search(request);
    }
}
