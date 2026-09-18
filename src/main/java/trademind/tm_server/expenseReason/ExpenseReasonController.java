package trademind.tm_server.expenseReason;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.expenseReason.dto.ExpenseReasonResponse;
import trademind.tm_server.expenseReason.dto.ExpenseReasonSearchRequest;
import trademind.tm_server.expenseReason.dto.NewExpenseReasonRequest;
import trademind.tm_server.expenseReason.dto.UpdateExpenseReasonRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_EXPENSE_REASON)
public class ExpenseReasonController {
    @Autowired
    private ExpenseReasonService service;

    @PostMapping
    public ExpenseReasonResponse create(@RequestBody NewExpenseReasonRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ExpenseReasonResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public ExpenseReasonResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public ExpenseReasonResponse update(
            @PathVariable Long id,
            @RequestBody UpdateExpenseReasonRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public ExpenseReasonResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<ExpenseReasonResponse> search(@RequestBody ExpenseReasonSearchRequest request) {
        return service.search(request);
    }
}
