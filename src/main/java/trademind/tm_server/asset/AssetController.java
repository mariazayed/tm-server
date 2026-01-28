package trademind.tm_server.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.asset.dto.AssetSearchRequest;
import trademind.tm_server.asset.dto.NewAssetRequest;
import trademind.tm_server.asset.dto.UpdateAssetRequest;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_ASSET)
public class AssetController {
    @Autowired
    private AssetService assetService;

    @PostMapping
    public AssetResponse createAsset(
            @RequestBody NewAssetRequest asset) {
        return assetService.create(asset);
    }

    @GetMapping
    public List<AssetResponse> getAllAssets() {
        return assetService.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public AssetResponse getAssetById(
            @PathVariable Long id) {
        return assetService.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public AssetResponse updateAsset(
            @PathVariable Long id,
            @RequestBody UpdateAssetRequest asset) {
        return assetService.update(id, asset);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public AssetResponse undoDeleteAsset(
            @PathVariable Long id) {
        return assetService.undoDelete(id);
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<AssetResponse> searchAssets(
            @RequestBody AssetSearchRequest request) {
        return assetService.search(request);
    }
}