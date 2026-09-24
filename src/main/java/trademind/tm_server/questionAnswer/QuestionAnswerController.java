package trademind.tm_server.questionAnswer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.questionAnswer.dto.NewQuestionAnswerRequest;
import trademind.tm_server.questionAnswer.dto.QuestionAnswerResponse;
import trademind.tm_server.questionAnswer.dto.QuestionAnswerSearchRequest;
import trademind.tm_server.questionAnswer.dto.UpdateQuestionAnswerRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_QUESTION_ANSWERS)
public class QuestionAnswerController {
    @Autowired
    private QuestionAnswerService service;

    @PostMapping
    public List<QuestionAnswerResponse> create(@RequestBody List<NewQuestionAnswerRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<QuestionAnswerResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public QuestionAnswerResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public QuestionAnswerResponse update(@PathVariable Long id, @RequestBody UpdateQuestionAnswerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<QuestionAnswerResponse> search(@RequestBody QuestionAnswerSearchRequest request) {
        return service.search(request);
    }
}
