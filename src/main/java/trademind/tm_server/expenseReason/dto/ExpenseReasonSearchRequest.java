package trademind.tm_server.expenseReason.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExpenseReasonSearchRequest extends BaseSearchRequest {
    private String name;
}
