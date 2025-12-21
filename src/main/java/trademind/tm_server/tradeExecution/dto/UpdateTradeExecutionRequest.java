package trademind.tm_server.tradeExecution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTradeExecutionRequest {
    /// ********** Timing & Session **********
    @NotNull(message = "Required")
    private LocalDateTime enteredAt;

    @NotNull(message = "Required")
    private SessionEnum session;

    /// ********** Core Trade Details **********
    @NotNull(message = "Required")
    private TradeDirectionEnum direction;

    @NotNull(message = "Required")
    private double lotSize;

    /// ********** Execution Logic **********
    @NotNull(message = "Required")
    private List<EntryReasonEnum> entryReasons;

    @NotNull(message = "Required")
    private List<EntryConfirmationEnum> entryConfirmations;

    @NotNull(message = "Required")
    private EntryTypeEnum entryType;

    /// ********** Classification **********
    @NotNull(message = "Required")
    private List<TradeTagEnum> tradeTags;

    /// ********** Additional Details **********
    private String notes;
}
