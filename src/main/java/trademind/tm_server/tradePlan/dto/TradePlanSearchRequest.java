package trademind.tm_server.tradePlan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "Request object for searching and filtering trading assets with pagination and sorting support")
public class TradePlanSearchRequest extends BaseSearchRequest {

    @Schema(
            description = "Filter by asset name (case-insensitive partial match supported)",
            example = "Bitcoin"
    )
    private String name;

    @Schema(
            description = "Filter by trading symbol or ticker (case-insensitive partial match supported). Can search for base or quote currency.",
            example = "BTCUSDT"
    )
    private String symbol;

    @Schema(
            description = "Filter by sector ID to find assets belonging to a specific market sector (e.g., DeFi, Layer 1)",
            example = "1"
    )
    private Long sectorId;

    @Schema(
            description = "Filter by trade type to find assets with specific trade types (e.g., SPOT, FUTURES)",
            example = "SPOT"
    )
    private String tradeType;

    @Schema(
            description = "Filter by trade status to find assets with active or closed trades (e.g., ACTIVE, CLOSED)",
            example = "ACTIVE"
    )
    private String tradeStatus;
}