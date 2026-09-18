package trademind.tm_server.expenseReason;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.expenseReason.dto.ExpenseReasonResponse;
import trademind.tm_server.expenseReason.dto.ExpenseReasonSearchRequest;
import trademind.tm_server.expenseReason.dto.NewExpenseReasonRequest;
import trademind.tm_server.expenseReason.dto.UpdateExpenseReasonRequest;
import trademind.tm_server.expenseReason.repository.ExpenseReasonRepository;
import trademind.tm_server.expenseReason.repository.ExpenseReasonSearchRepository;
import trademind.tm_server.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseReasonService {

    @Autowired
    private ExpenseReasonRepository repository;

    @Autowired
    private ExpenseReasonSearchRepository searchRepository;

    @Autowired
    private ExpenseReasonMapper mapper;

    private final String tableName = "ExpenseReason";

    public ExpenseReasonResponse create(NewExpenseReasonRequest request) {
        request.setName(StringUtils.capitalizeFirst(request.getName()));

        ExpenseReason entity = mapper.toEntity(request);
        ExpenseReason saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public List<ExpenseReasonResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ExpenseReasonResponse getOneById(Long id) {
        ExpenseReason entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(entity);
    }

    // Used by ExpenseService to resolve/validate the chosen reason before
    // attaching it to an Expense (mirrors AssetService.getOneByIdAsEntity).
    public ExpenseReason getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public ExpenseReasonResponse update(Long id, UpdateExpenseReasonRequest details) {
        ExpenseReason entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (details.getName() != null) {
            entity.setName(StringUtils.capitalizeFirst(details.getName()));
        }

        // Save the fetched-and-mutated entity, not a fresh mapper.toEntity()
        // of the update DTO -- see AssetService.update()'s comment on the
        // duplicate-row bug that pattern caused there.
        ExpenseReason saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public ExpenseReasonResponse undoDelete(Long id) {
        ExpenseReason entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (entity.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        entity.setDeletedAt(null);
        ExpenseReason saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    // Soft-delete: existing expenses keep pointing at this reason and keep
    // showing it (their FK stays intact, and getOneById/toResponse doesn't
    // filter on the reason's deletedAt), it just drops out of the active
    // dropdown/list for new expenses.
    public void delete(Long id) {
        ExpenseReason entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }

    public SearchResponse<ExpenseReasonResponse> search(ExpenseReasonSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getName() != null) filters.put("name", request.getName());

        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<ExpenseReason> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}
