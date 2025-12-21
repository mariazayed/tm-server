package trademind.tm_server.tradeEvaluate.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;
import trademind.tm_server.enums.EntryReasonEnum;
import trademind.tm_server.enums.TradeDirectionEnum;

@EqualsAndHashCode(callSuper = true)
@Data
public class TradeEvaluateSearchRequest extends BaseSearchRequest {
    private TradeDirectionEnum direction;
    private EntryReasonEnum entryReason;
}