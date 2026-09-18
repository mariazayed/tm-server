package trademind.tm_server.expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.expense.dto.ExpenseResponse;
import trademind.tm_server.expense.dto.ExpenseSearchRequest;
import trademind.tm_server.expense.dto.NewExpenseRequest;
import trademind.tm_server.expense.dto.UpdateExpenseRequest;
import trademind.tm_server.expense.repository.ExpenseRepository;
import trademind.tm_server.expense.repository.ExpenseSearchRepository;
import trademind.tm_server.expenseReason.ExpenseReason;
import trademind.tm_server.expenseReason.ExpenseReasonService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private ExpenseSearchRepository searchRepository;

    @Autowired
    private ExpenseMapper mapper;

    @Autowired
    private ExpenseReasonService expenseReasonService;

    private final String tableName = "Expense";

    public ExpenseResponse create(NewExpenseRequest request) {
        ExpenseReason reason = expenseReasonService.getOneByIdAsEntity(request.getExpenseReasonId());

        Expense expense = mapper.toEntity(request);
        expense.setExpenseReason(reason);

        Expense saved = repository.save(expense);
        return mapper.toResponse(saved);
    }

    public List<ExpenseResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ExpenseResponse getOneById(Long id) {
        Expense expense = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(expense);
    }

    public ExpenseResponse update(Long id, UpdateExpenseRequest details) {
        Expense expense = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (details.getDate() != null) {
            expense.setDate(details.getDate());
        }

        if (details.getAmount() != null) {
            expense.setAmount(details.getAmount());
        }

        if (details.getNotes() != null) {
            expense.setNotes(details.getNotes());
        }

        if (details.getExpenseReasonId() != null) {
            ExpenseReason reason = expenseReasonService.getOneByIdAsEntity(details.getExpenseReasonId());
            expense.setExpenseReason(reason);
        }

        // Save the fetched-and-mutated entity, not a fresh mapper.toEntity()
        // of the update DTO -- see AssetService.update()'s comment on the
        // duplicate-row bug that pattern caused there.
        Expense saved = repository.save(expense);
        return mapper.toResponse(saved);
    }

    public ExpenseResponse undoDelete(Long id) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (expense.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        expense.setDeletedAt(null);
        Expense saved = repository.save(expense);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Expense expense = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        expense.setDeletedAt(LocalDateTime.now());
        repository.save(expense);
    }

    // Real, server-side pagination, same reasoning as Asset/Goal's search():
    // this list only grows over time, so the Expenses screen pages through
    // it instead of one unbounded GET. Optional expenseReasonId filter for
    // "show me only Rent expenses"-style use from the UI later.
    public SearchResponse<ExpenseResponse> search(ExpenseSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        if (request.getExpenseReasonId() != null) {
            filters.put("expenseReasonId", request.getExpenseReasonId());
            relationFilters.put("expenseReason", "expenseReasonId");
        }

        BaseSearchService<Expense> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}
