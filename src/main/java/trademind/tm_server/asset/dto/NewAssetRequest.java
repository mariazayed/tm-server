package trademind.tm_server.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewAssetRequest {
    @NotNull(message = "Asset name is required")
    private String name;

    @NotNull(message = "Asset symbol is required")
    private String symbol;

    private Double contractSize;

    private Double pipSize;
}
