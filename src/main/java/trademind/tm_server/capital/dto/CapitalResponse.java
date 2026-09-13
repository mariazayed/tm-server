package trademind.tm_server.capital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapitalResponse {
    private LocalDateTime dateTime;
    private double amount;
    private CapitalReasonEnum reason;
    private String notes;
    private String externalId;
}
