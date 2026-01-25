package trademind.tm_server.asset;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.asset.dto.NewAssetRequest;
import trademind.tm_server.asset.dto.UpdateAssetRequest;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetResponse toResponse(Asset asset);

    Asset toEntity(NewAssetRequest request);

    Asset toEntity(UpdateAssetRequest request);


    default Asset map(Long id) {
        if (id == null) {
            return null;
        }
        Asset asset = new Asset();
        asset.setId(id);
        return asset;
    }
}

