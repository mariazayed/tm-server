package trademind.tm_server.trade;//package trademind.tm_server.tradePlan;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import trader.beast.traderBeast.asset.dto.AssetResponse;
//import trader.beast.traderBeast.asset.dto.AssetSearchRequest;
//import trader.beast.traderBeast.asset.dto.NewAssetRequest;
//import trader.beast.traderBeast.asset.dto.UpdateAssetRequest;
//import trader.beast.traderBeast.asset.repository.AssetRepository;
//import trader.beast.traderBeast.asset.repository.AssetSearchRepository;
//import trader.beast.traderBeast.base.BaseSearchService;
//import trader.beast.traderBeast.base.SearchResponse;
//import trader.beast.traderBeast.exceptions.BadRequestException;
//import trader.beast.traderBeast.exceptions.ResourceNotFoundException;
//import trader.beast.traderBeast.utils.StringUtils;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class TradePlanService {
//
//    @Autowired
//    private AssetRepository repository;
//
//    @Autowired
//    private AssetSearchRepository searchRepository;
//
//    @Autowired
//    private TradePlanMapper mapper;
//
//    public AssetResponse create(NewAssetRequest asset) {
//        String name = StringUtils.capitalizeFirst(asset.getName());
//        String symbol = asset.getSymbol().toUpperCase();
//
//        asset.setName(name);
//        asset.setSymbol(symbol);
//
//        TradePlan tradePlanEntity = mapper.toEntity(asset);
//        TradePlan savedTradePlan = repository.save(tradePlanEntity);
//        return mapper.toResponse(savedTradePlan);
//    }
//
//    public List<AssetResponse> getAll() {
//        List<TradePlan> tradePlanList = repository.findByDeletedAtIsNull();
//
//        return tradePlanList.stream()
//                .map(mapper::toResponse) // convert each Asset to AssetResponse
//                .toList();
//    }
//
//    public AssetResponse getOneById(Long id) {
//        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
//
//        return mapper.toResponse(tradePlan);
//    }
//
//    public TradePlan getOneByIdAsEntity(Long id) {
//        return repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
//    }
//
//    public AssetResponse update(Long id, UpdateAssetRequest assetDetails) {
//        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
//
//        if (assetDetails.getName() != null) {
//            String name = StringUtils.capitalizeFirst(assetDetails.getName());
//            tradePlan.setName(name);
//        }
//
//        if (assetDetails.getSymbol() != null) {
//            String symbol = assetDetails.getSymbol().toUpperCase();
//            tradePlan.setSymbol(symbol);
//        }
//
//        TradePlan tradePlanEntity = mapper.toEntity(assetDetails);
//        TradePlan savedTradePlan = repository.save(tradePlanEntity);
//        return mapper.toResponse(savedTradePlan);
//    }
//
//    public AssetResponse undoDelete(Long id) {
//        TradePlan tradePlan = repository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
//
//        if (tradePlan.getDeletedAt() == null) {
//            throw new BadRequestException("Asset with id " + id + " is not deleted");
//        }
//
//        // Undo the delete
//        tradePlan.setDeletedAt(null);
//
//        // Save and map to response
//        TradePlan savedTradePlan = repository.save(tradePlan);
//        return mapper.toResponse(savedTradePlan);
//    }
//
//    public void delete(Long id) {
//        TradePlan tradePlan = repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
//        tradePlan.setDeletedAt(LocalDateTime.now());
//        repository.save(tradePlan);
//    }
//
//    public SearchResponse<AssetResponse> search(AssetSearchRequest request) {
//
//        Map<String, Object> filters = new HashMap<>();
//        if (request.getName() != null) filters.put("name", request.getName());
//        if (request.getSymbol() != null) filters.put("symbol", request.getSymbol());
//        if (request.getSectorId() != null) filters.put("sectorId", request.getSectorId());
//
//        Map<String, String> relationFilters = new HashMap<>();
//        relationFilters.put("sectors", "sectorId");
//
//        BaseSearchService<TradePlan> baseService = new BaseSearchService<>(searchRepository);
//
//        return baseService.search(
//                request,
//                filters,
//                relationFilters,
//                mapper::toResponse
//        );
//    }
//}