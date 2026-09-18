package trademind.tm_server.expense;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import trademind.tm_server.expense.dto.ExpenseResponse;
import trademind.tm_server.expense.dto.NewExpenseRequest;
import trademind.tm_server.expenseReason.ExpenseReasonMapper;

// `uses = ExpenseReasonMapper.class` lets toResponse() auto-map the nested
// expenseReason field (Expense.expenseReason : ExpenseReason ->
// ExpenseResponse.expenseReason : ExpenseReasonResponse) via
// ExpenseReasonMapper.toResponse(), same composition style as
// CapitalMapper's `uses = AssetMapper.class`.
//
// toEntity() deliberately ignores expenseReason: ExpenseService resolves and
// validates the chosen reason itself (via ExpenseReasonService, throwing a
// proper 404 if the id doesn't exist) and sets it on the entity explicitly,
// rather than relying on MapStruct's Long->entity id-only reference.
@Mapper(componentModel = "spring", uses = ExpenseReasonMapper.class)
public interface ExpenseMapper {
    ExpenseResponse toResponse(Expense expense);

    @Mapping(target = "expenseReason", ignore = true)
    Expense toEntity(NewExpenseRequest request);
}
