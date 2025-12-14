package trademind.tm_server.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Sort;

/**
 * Base class for search request objects providing common pagination and sorting functionality.
 * All search request DTOs should extend this class to inherit standard search capabilities.
 *
 * <p>Provides:
 * <ul>
 *   <li>Page-based pagination with configurable page size</li>
 *   <li>Field-based sorting with ascending or descending order</li>
 *   <li>Sensible defaults (page 0, size 10, sorted by ID ascending)</li>
 * </ul>
 *
 * <p>Usage: Extend this class and add specific filter fields for your entity.
 *
 * @see org.springframework.data.domain.Pageable
 * @see Sort
 */
@Data
@Schema(description = "Base request object for search operations with pagination and sorting capabilities")
public class BaseSearchRequest {

    @Schema(
            description = "Page number for pagination (zero-based index). First page is 0, second page is 1, etc.",
            example = "0",
            defaultValue = "0"
    )
    private int page = 0;

    @Schema(
            description = "Number of items to return per page. Controls the page size for paginated results.",
            example = "10",
            defaultValue = "10"
    )
    private int size = 10;

    @Schema(
            description = "Name of the field to sort results by. Should match entity field names (e.g., 'id', 'name', 'createdAt').",
            example = "id",
            defaultValue = "id"
    )
    private String sortBy = "id";

    @Schema(
            description = "Sort direction. ASC for ascending order (A-Z, 0-9, oldest first) or DESC for descending order (Z-A, 9-0, newest first).",
            example = "ASC",
            defaultValue = "ASC",
            allowableValues = {"ASC", "DESC"}
    )
    private Sort.Direction direction = Sort.Direction.ASC;
}