package trademind.tm_server.expense.dto;

import lombok.Data;
import trademind.tm_server.expenseReason.dto.ExpenseReasonResponse;

import java.time.LocalDateTime;

@Data
public class ExpenseResponse {
    private Long id;

    // No @JsonFormat here -- default Jackson LocalDateTime serialization
    // (ISO-8601 with a 'T' separator) round-trips fine with Dart's
    // DateTime.parse, same as GoalResponse's startDate/endDate. The space-
    // separator fix below only matters for *deserializing* what Flutter
    // sends on create/update.
    private LocalDateTime date;

    private double amount;

    private ExpenseReasonResponse expenseReason;

    private String notes;
}
