package trademind.tm_server.asset;

import org.mapstruct.Mapper;
import trader.beast.traderBeast.asset.dto.AssetResponse;
import trader.beast.traderBeast.asset.dto.NewAssetRequest;
import trader.beast.traderBeast.asset.dto.UpdateAssetRequest;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetResponse toResponse(Asset asset);

    Asset toEntity(NewAssetRequest request);

    Asset toEntity(UpdateAssetRequest request);
}

