package trademind.tm_server.base;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Map;

@RequiredArgsConstructor
public class BaseSearchService<T> {

    private final JpaSpecificationExecutor<T> repository;

    /**
     * Generic search method with mapper
     */
    public <R> SearchResponse<R> search(
            BaseSearchRequest request,
            Map<String, Object> filters,
            Map<String, String> relationFilters,
            java.util.function.Function<T, R> mapper) {
        try {
            // Add deletedAt filter by default
            if (!filters.containsKey("deletedAt")) {
                filters.put("deletedAt", null);
            }

            var spec = new BaseSpecification<T>(filters, relationFilters);

            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize(),
                    Sort.by(request.getDirection(), request.getSortBy())
            );

            var page = repository.findAll(spec, pageable);

            // Map each entity to the DTO
            var mappedData = page.getContent().stream()
                    .map(mapper)
                    .toList();

            return SearchResponse.success(
                    mappedData,
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isLast()
            );
        } catch (Exception e) {
            return SearchResponse.error(e.getMessage());
        }
    }
}

