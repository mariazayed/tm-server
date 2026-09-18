package trademind.tm_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewExpenseRequest {

    // Fix applied proactively (see decisions-log-feature-implementation.md's
    // "LocalDateTime deserialization" note): Jackson's default LocalDateTime
    // parser only accepts the ISO 'T' separator, but every *Model in
    // tm_dashboard sends 'yyyy-MM-dd HH:mm:ss' (space-separated) via
    // DateFormat('yyyy-MM-dd HH:mm:ss'). Without this, POST /expense would
    // throw HttpMessageNotReadableException the same way decision creation
    // did before it was fixed there.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    private double amount;

    private Long expenseReasonId;

    private String notes;
}
