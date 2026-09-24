package trademind.tm_server.questionCategory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.questionCategory.dto.NewQuestionCategoryRequest;
import trademind.tm_server.questionCategory.dto.QuestionCategoryResponse;
import trademind.tm_server.questionCategory.dto.QuestionCategorySearchRequest;
import trademind.tm_server.questionCategory.dto.UpdateQuestionCategoryRequest;
import trademind.tm_server.questionCategory.repository.QuestionCategoryRepository;
import trademind.tm_server.questionCategory.repository.QuestionCategorySearchRepository;
import trademind.tm_server.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionCategoryService {
    @Autowired
    private QuestionCategoryRepository repository;

    @Autowired
    private QuestionCategorySearchRepository searchRepository;

    @Autowired
    private QuestionCategoryMapper mapper;

    private final String tableName = "QuestionCategory";

    public QuestionCategoryResponse create(NewQuestionCategoryRequest request) {
        request.setName(StringUtils.capitalizeFirst(request.getName()));

        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    public List<QuestionCategoryResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public QuestionCategoryResponse getOneById(Long id) {
        return mapper.toResponse(getOneByIdAsEntity(id));
    }

    public QuestionCategory getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public QuestionCategoryResponse update(Long id, UpdateQuestionCategoryRequest request) {
        QuestionCategory category = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (request.getName() != null) {
            category.setName(StringUtils.capitalizeFirst(request.getName()));
        }

        return mapper.toResponse(repository.save(category));
    }

    public void delete(Long id) {
        QuestionCategory category = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        category.setDeletedAt(LocalDateTime.now());
        repository.save(category);
    }

    public SearchResponse<QuestionCategoryResponse> search(QuestionCategorySearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getName() != null) filters.put("name", request.getName());

        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<QuestionCategory> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}
