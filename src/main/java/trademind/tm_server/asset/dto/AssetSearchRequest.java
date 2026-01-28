package trademind.tm_server.asset.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssetSearchRequest extends BaseSearchRequest {

    private String name;
    private String symbol;
}