package trademind.tm_server.expense.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExpenseSearchRequest extends BaseSearchRequest {
    // Optional filter to a single reason -- resolved via BaseSpecification's
    // relationFilters (joins the "expenseReason" relation and matches its
    // id), same mechanism the base class's javadoc already documents for
    // many-to-many joins, applied here to a many-to-one instead.
    private Long expenseReasonId;
}
