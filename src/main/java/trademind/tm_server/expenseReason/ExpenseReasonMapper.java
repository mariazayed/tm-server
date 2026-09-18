package trademind.tm_server.expenseReason;

import org.mapstruct.Mapper;
import trademind.tm_server.expenseReason.dto.ExpenseReasonResponse;
import trademind.tm_server.expenseReason.dto.NewExpenseReasonRequest;

@Mapper(componentModel = "spring")
public interface ExpenseReasonMapper {
    ExpenseReasonResponse toResponse(ExpenseReason expenseReason);

    ExpenseReason toEntity(NewExpenseReasonRequest request);

    // Lets ExpenseMapper (uses = ExpenseReasonMapper.class) convert a raw
    // Long id into an ExpenseReason reference -- same pattern as
    // AssetMapper.map(Long), used the same way by TradeMapper for assetId.
    default ExpenseReason map(Long id) {
        if (id == null) {
            return null;
        }
        ExpenseReason expenseReason = new ExpenseReason();
        expenseReason.setId(id);
        return expenseReason;
    }
}
