package trademind.tm_server.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.asset.dto.AssetSearchRequest;
import trademind.tm_server.asset.dto.NewAssetRequest;
import trademind.tm_server.asset.dto.UpdateAssetRequest;
import trademind.tm_server.asset.repository.AssetRepository;
import trademind.tm_server.asset.repository.AssetSearchRepository;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetService {

    @Autowired
    private AssetRepository repository;

    @Autowired
    private AssetSearchRepository searchRepository;

    @Autowired
    private AssetMapper mapper;

    public AssetResponse create(NewAssetRequest asset) {
        String name = StringUtils.capitalizeFirst(asset.getName());
        String symbol = asset.getSymbol().toUpperCase();

        asset.setName(name);
        asset.setSymbol(symbol);

        Asset assetEntity = mapper.toEntity(asset);
        Asset savedAsset = repository.save(assetEntity);
        return mapper.toResponse(savedAsset);
    }

    public List<AssetResponse> getAll() {
        List<Asset> assetList = repository.findByDeletedAtIsNull();

        return assetList.stream()
                .map(mapper::toResponse) // convert each Asset to AssetResponse
                .toList();
    }

    public AssetResponse getOneById(Long id) {
        Asset asset = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));

        return mapper.toResponse(asset);
    }

    public Asset getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
    }

    public AssetResponse update(Long id, UpdateAssetRequest assetDetails) {
        Asset asset = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));

        if (assetDetails.getName() != null) {
            String name = StringUtils.capitalizeFirst(assetDetails.getName());
            asset.setName(name);
        }

        if (assetDetails.getSymbol() != null) {
            String symbol = assetDetails.getSymbol().toUpperCase();
            asset.setSymbol(symbol);
        }

        Asset assetEntity = mapper.toEntity(assetDetails);
        Asset savedAsset = repository.save(assetEntity);
        return mapper.toResponse(savedAsset);
    }

    public AssetResponse undoDelete(Long id) {
        Asset asset = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));

        if (asset.getDeletedAt() == null) {
            throw new BadRequestException("Asset with id " + id + " is not deleted");
        }

        // Undo the delete
        asset.setDeletedAt(null);

        // Save and map to response
        Asset savedAsset = repository.save(asset);
        return mapper.toResponse(savedAsset);
    }

    public void delete(Long id) {
        Asset asset = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
        asset.setDeletedAt(LocalDateTime.now());
        repository.save(asset);
    }

    public SearchResponse<AssetResponse> search(AssetSearchRequest request) {

        Map<String, Object> filters = new HashMap<>();
        if (request.getName() != null) filters.put("name", request.getName());
        if (request.getSymbol() != null) filters.put("symbol", request.getSymbol());
        if (request.getSectorId() != null) filters.put("sectorId", request.getSectorId());

        Map<String, String> relationFilters = new HashMap<>();
        relationFilters.put("sectors", "sectorId");

        BaseSearchService<Asset> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(
                request,
                filters,
                relationFilters,
                mapper::toResponse
        );
    }
}