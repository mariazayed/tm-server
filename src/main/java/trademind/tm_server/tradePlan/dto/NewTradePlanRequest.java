package trademind.tm_server.tradePlan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTradePlanRequest {
    @NotNull(message = "Asset name is required")
    private String name;

    @NotNull(message = "Asset symbol is required")
    private String symbol;
}
