package trademind.tm_server.tradeExecution.dto;

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
public class TradeExecutionResponse {
    private int id;
    private String notes;

    /// ********** Timing & Session **********
    private LocalDateTime enteredAt;

    private SessionEnum session;

    /// ********** Core Trade Details **********
    private TradeDirectionEnum direction;

    private double lotSize;

    /// ********** Execution Logic **********
    private List<EntryReasonEnum> entryReasons;

    private List<EntryConfirmationEnum> entryConfirmations;

    private EntryTypeEnum entryType;

    /// ********** Classification **********
    private List<TradeTagEnum> tradeTags;
}
