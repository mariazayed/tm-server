package trademind.tm_server.asset;

import org.springframework.stereotype.Component;
import trademind.tm_server.asset.repository.AssetRepository;

@Component
public class AssetLookupMapper {

    private final AssetRepository assetRepository;

    public AssetLookupMapper(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset map(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Asset not found with id: " + assetId)
                );
    }
}
