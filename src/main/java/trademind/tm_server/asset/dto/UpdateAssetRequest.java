package trademind.tm_server.asset.dto;

import lombok.Data;

@Data
public class UpdateAssetRequest {
    private String name;
    private String symbol;
    private Double contractSize;
    private Double pipSize;
}
