package trademind.tm_server.questionCategory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.questionCategory.dto.NewQuestionCategoryRequest;
import trademind.tm_server.questionCategory.dto.QuestionCategoryResponse;
import trademind.tm_server.questionCategory.dto.QuestionCategorySearchRequest;
import trademind.tm_server.questionCategory.dto.UpdateQuestionCategoryRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_QUESTION_CATEGORIES)
public class QuestionCategoryController {
    @Autowired
    private QuestionCategoryService service;

    @PostMapping
    public QuestionCategoryResponse create(@RequestBody NewQuestionCategoryRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<QuestionCategoryResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public QuestionCategoryResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public QuestionCategoryResponse update(@PathVariable Long id, @RequestBody UpdateQuestionCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<QuestionCategoryResponse> search(@RequestBody QuestionCategorySearchRequest request) {
        return service.search(request);
    }
}
