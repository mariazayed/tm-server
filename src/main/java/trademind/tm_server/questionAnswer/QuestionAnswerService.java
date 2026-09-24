package trademind.tm_server.questionAnswer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.questionAnswer.dto.NewQuestionAnswerRequest;
import trademind.tm_server.questionAnswer.dto.QuestionAnswerResponse;
import trademind.tm_server.questionAnswer.dto.QuestionAnswerSearchRequest;
import trademind.tm_server.questionAnswer.dto.UpdateQuestionAnswerRequest;
import trademind.tm_server.questionAnswer.repository.QuestionAnswerRepository;
import trademind.tm_server.questionAnswer.repository.QuestionAnswerSearchRepository;
import trademind.tm_server.questionCategory.QuestionCategory;
import trademind.tm_server.questionCategory.QuestionCategoryService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionAnswerService {
    @Autowired
    private QuestionAnswerRepository repository;

    @Autowired
    private QuestionAnswerSearchRepository searchRepository;

    @Autowired
    private QuestionAnswerMapper mapper;

    @Autowired
    private QuestionCategoryService categoryService;

    private final String tableName = "QuestionAnswer";

    public List<QuestionAnswerResponse> create(List<NewQuestionAnswerRequest> requests) {
        List<QuestionAnswer> saved = repository.saveAll(
                requests.stream().map(request -> {
                    QuestionAnswer questionAnswer = mapper.toEntity(request);
                    questionAnswer.setCategory(resolveCategory(request.getCategoryId()));
                    return questionAnswer;
                }).toList()
        );

        return saved.stream().map(mapper::toResponse).toList();
    }

    public List<QuestionAnswerResponse> getAll() {
        return repository.findByDeletedAtIsNull().stream()
                .sorted(Comparator.comparing(QuestionAnswer::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(mapper::toResponse)
                .toList();
    }

    public QuestionAnswerResponse getOneById(Long id) {
        QuestionAnswer questionAnswer = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(questionAnswer);
    }

    public QuestionAnswerResponse update(Long id, UpdateQuestionAnswerRequest request) {
        QuestionAnswer questionAnswer = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        mapper.updateEntity(request, questionAnswer);
        questionAnswer.setCategory(resolveCategory(request.getCategoryId()));

        return mapper.toResponse(repository.save(questionAnswer));
    }

    public void delete(Long id) {
        QuestionAnswer questionAnswer = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        questionAnswer.setDeletedAt(LocalDateTime.now());
        repository.save(questionAnswer);
    }

    public SearchResponse<QuestionAnswerResponse> search(QuestionAnswerSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<QuestionAnswer> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }

    private QuestionCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryService.getOneByIdAsEntity(categoryId);
    }
}
