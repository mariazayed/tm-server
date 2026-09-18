package trademind.tm_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateExpenseRequest {

    // See NewExpenseRequest.date for why this annotation is required.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    // Boxed (nullable) so ExpenseService.update() can tell "not sent" apart
    // from "explicitly set to 0" -- same reasoning as UpdateAssetRequest's
    // fields all being optional/nullable for a partial update.
    private Double amount;

    private Long expenseReasonId;

    private String notes;
}
