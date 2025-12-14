package trademind.tm_server.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseWrapper {

    /**
     * Wrap a Spring Data Page into a standardized SearchResponse with pagination info.
     * Status will be "success".
     */
    public static <T> SearchResponse<T> success(Page<T> page) {
        return SearchResponse.success(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Wrap a list into a standardized SearchResponse without pagination.
     * Useful for endpoints that return all results.
     */
    public static <T> SearchResponse<T> success(List<T> data) {
        return SearchResponse.success(
                data,
                0,
                data.size(),
                data.size(),
                1,
                true
        );
    }

    /**
     * Wrap an error message into a standardized SearchResponse.
     * Status will be "error".
     */
    public static <T> SearchResponse<T> error(String errorMessage) {
        return SearchResponse.error(errorMessage);
    }
}
