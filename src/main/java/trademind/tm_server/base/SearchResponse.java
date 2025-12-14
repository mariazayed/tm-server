package trademind.tm_server.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper for paginated search responses.
 * Provides a consistent structure for all search/list endpoints that support pagination.
 *
 * <p>Includes pagination metadata (current page, total pages, total elements) along with
 * the actual data results and status information.
 *
 * <p>Usage:
 * <ul>
 *   <li>Success responses: Use {@link #success(List, int, int, long, int, boolean)} factory method</li>
 *   <li>Error responses: Use {@link #error(String)} factory method</li>
 * </ul>
 *
 * @param <T> The type of data items contained in the response
 * @see BaseSearchRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic paginated search response wrapper containing results and pagination metadata")
public class SearchResponse<T> {

    @Schema(
            description = "Response status indicating success or error",
            example = "success",
            allowableValues = {"success", "error"}
    )
    private String status;

    @Schema(
            description = "List of data items for the current page. Null if status is 'error'."
    )
    private List<T> data;

    @Schema(
            description = "Current page number (zero-based index). First page is 0, second page is 1, etc.",
            example = "0"
    )
    private int page;

    @Schema(
            description = "Number of items per page. Matches the size parameter from the request.",
            example = "10"
    )
    private int size;

    @Schema(
            description = "Total number of items matching the search criteria across all pages",
            example = "100"
    )
    private long totalElements;

    @Schema(
            description = "Total number of pages available based on size and totalElements",
            example = "10"
    )
    private int totalPages;

    @Schema(
            description = "Indicates whether this is the last page of results. True if no more pages are available.",
            example = "false"
    )
    private boolean last;

    @Schema(
            description = "Error message describing what went wrong. Only present when status is 'error', null otherwise.",
            example = "Invalid search parameters provided"
    )
    private String errorMessage;

    /**
     * Factory method to create a successful search response.
     * Sets status to "success" and errorMessage to null.
     *
     * @param <T>           Type of data items
     * @param data          List of results for the current page
     * @param page          Current page number (zero-based)
     * @param size          Number of items per page
     * @param totalElements Total number of items across all pages
     * @param totalPages    Total number of pages
     * @param last          Whether this is the last page
     * @return SearchResponse with success status and provided data
     */
    public static <T> SearchResponse<T> success(List<T> data, int page, int size, long totalElements, int totalPages, boolean last) {
        return new SearchResponse<>("success", data, page, size, totalElements, totalPages, last, null);
    }

    /**
     * Factory method to create an error search response.
     * Sets status to "error", data to null, and pagination metadata to zero/false.
     *
     * @param <T>     Type of data items
     * @param message Error message describing what went wrong
     * @return SearchResponse with error status and provided error message
     */
    public static <T> SearchResponse<T> error(String message) {
        return new SearchResponse<>("error", null, 0, 0, 0, 0, false, message);
    }
}