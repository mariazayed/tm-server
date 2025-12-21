package trademind.tm_server.trade;//package trademind.tm_server.tradePlan;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@CrossOrigin(origins = "http://localhost:4200")
//@RestController
//@RequestMapping(AppConstants.URL_ASSET)
//@Tag(name = "Asset", description = "API endpoints for managing trading assets and cryptocurrencies")
//public class TradePlanController {
//    @Autowired
//    private TradePlanService tradePlanService;
//
//    @PostMapping
//    @Operation(
//            summary = "Create a new asset",
//            description = "Creates a new trading asset (cryptocurrency or token) in the system with name and symbol"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Asset created successfully",
//                    content = @Content(schema = @Schema(implementation = TradePlan.class))
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid asset data provided"
//            )
//    })
//    public AssetResponse createAsset(
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = "Asset object to be created with name and trading symbol",
//                    required = true
//            )
//            @RequestBody NewAssetRequest asset) {
//        return tradePlanService.create(asset);
//    }
//
//    @GetMapping
//    @Operation(
//            summary = "Get all assets",
//            description = "Retrieves a list of all trading assets in the system"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Successfully retrieved all assets",
//                    content = @Content(schema = @Schema(implementation = TradePlan.class))
//            )
//    })
//    public List<AssetResponse> getAllAssets() {
//        return tradePlanService.getAll();
//    }
//
//    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
//    @Operation(
//            summary = "Get asset by ID",
//            description = "Retrieves a specific trading asset by its unique identifier"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Asset found",
//                    content = @Content(schema = @Schema(implementation = TradePlan.class))
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Asset not found with the given ID"
//            )
//    })
//    public AssetResponse getAssetById(
//            @Parameter(description = "ID of the asset to retrieve", example = "1", required = true)
//            @PathVariable Long id) {
//        return tradePlanService.getOneById(id);
//    }
//
//    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
//    @Operation(
//            summary = "Update asset",
//            description = "Updates an existing asset with new information such as name, symbol, or sector associations"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Asset updated successfully",
//                    content = @Content(schema = @Schema(implementation = TradePlan.class))
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Asset not found with the given ID"
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid asset data provided"
//            )
//    })
//    public AssetResponse updateAsset(
//            @Parameter(description = "ID of the asset to update", example = "1", required = true)
//            @PathVariable Long id,
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = "Updated asset object with modified fields",
//                    required = true
//            )
//            @RequestBody UpdateAssetRequest asset) {
//        return tradePlanService.update(id, asset);
//    }
//
//    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
//    @Operation(
//            summary = "Delete asset",
//            description = "Soft deletes an asset by marking it as deleted without removing it from the database"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "204",
//                    description = "Asset deleted successfully"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Asset not found with the given ID"
//            )
//    })
//    public ResponseEntity<Void> deleteAsset(
//            @Parameter(description = "ID of the asset to delete", example = "1", required = true)
//            @PathVariable Long id) {
//        tradePlanService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
//    @Operation(
//            summary = "Restore deleted asset",
//            description = "Restores a previously soft-deleted asset, making it active again"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Asset restored successfully",
//                    content = @Content(schema = @Schema(implementation = TradePlan.class))
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Asset not found with the given ID"
//            )
//    })
//    public AssetResponse undoDeleteAsset(
//            @Parameter(description = "ID of the asset to restore", example = "1", required = true)
//            @PathVariable Long id) {
//        return tradePlanService.undoDelete(id);
//    }
//
//    @PostMapping(AppConstants.SUB_URL_SEARCH)
//    @Operation(
//            summary = "Search assets",
//            description = "Searches and filters assets based on various criteria such as name, symbol, sector, trade type, and trade status with pagination support"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Search completed successfully",
//                    content = @Content(schema = @Schema(implementation = SearchResponse.class))
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid search criteria"
//            )
//    })
//    public SearchResponse<AssetResponse> searchAssets(
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = "Search criteria including filters for name, symbol, sector ID, trade type, and trade status",
//                    required = true
//            )
//            @RequestBody AssetSearchRequest request) {
//        return tradePlanService.search(request);
//    }
//}