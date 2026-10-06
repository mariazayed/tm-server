package trademind.tm_server.invest.learning;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.invest.journal.JournalDtos.TagResponse;
import trademind.tm_server.invest.learning.LearningDtos.CardRequest;
import trademind.tm_server.invest.learning.LearningDtos.CardResponse;
import trademind.tm_server.invest.learning.LearningDtos.CardStats;
import trademind.tm_server.invest.learning.LearningDtos.NotePage;
import trademind.tm_server.invest.learning.LearningDtos.NoteRequest;
import trademind.tm_server.invest.learning.LearningDtos.NoteResponse;
import trademind.tm_server.invest.learning.LearningDtos.ReviewRequest;
import trademind.tm_server.invest.learning.LearningDtos.SourceRequest;
import trademind.tm_server.invest.learning.LearningDtos.SourceResponse;

import java.util.List;

/** Under /api/v1/invest, so every route needs a valid login token. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/learning")
public class LearningController {

    private final LearningService learning;
    private final FlashcardService flashcards;

    public LearningController(LearningService learning, FlashcardService flashcards) {
        this.learning = learning;
        this.flashcards = flashcards;
    }

    // sources
    @GetMapping("/sources")
    public List<SourceResponse> sources(@RequestParam(required = false) SourceStatus status) {
        return learning.listSources(status);
    }

    @GetMapping("/sources/{id}")
    public SourceResponse source(@PathVariable Long id) {
        return learning.getSource(id);
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public SourceResponse createSource(@Valid @RequestBody SourceRequest request) {
        return learning.createSource(request);
    }

    @PutMapping("/sources/{id}")
    public SourceResponse updateSource(@PathVariable Long id, @Valid @RequestBody SourceRequest request) {
        return learning.updateSource(id, request);
    }

    @DeleteMapping("/sources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable Long id) {
        learning.deleteSource(id);
    }

    // notes
    @GetMapping("/notes")
    public NotePage notes(@RequestParam(required = false) Long sourceId,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "30") int size) {
        return learning.listNotes(new LearningService.NoteFilter(sourceId, tag, q), page, size);
    }

    @GetMapping("/notes/tags")
    public List<TagResponse> noteTags() {
        return learning.noteTags();
    }

    @GetMapping("/notes/{id}")
    public NoteResponse note(@PathVariable Long id) {
        return learning.getNote(id);
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse createNote(@Valid @RequestBody NoteRequest request) {
        return learning.createNote(request);
    }

    @PutMapping("/notes/{id}")
    public NoteResponse updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return learning.updateNote(id, request);
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long id) {
        learning.deleteNote(id);
    }

    // flashcards
    @GetMapping("/cards")
    public List<CardResponse> cards(@RequestParam(required = false) Long noteId) {
        return flashcards.list(noteId);
    }

    @GetMapping("/cards/due")
    public List<CardResponse> dueCards(@RequestParam(defaultValue = "20") int limit) {
        return flashcards.due(limit);
    }

    @GetMapping("/cards/stats")
    public CardStats cardStats() {
        return flashcards.stats();
    }

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(@Valid @RequestBody CardRequest request) {
        return flashcards.create(request);
    }

    @PutMapping("/cards/{id}")
    public CardResponse updateCard(@PathVariable Long id, @Valid @RequestBody CardRequest request) {
        return flashcards.update(id, request);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id) {
        flashcards.delete(id);
    }

    @PostMapping("/cards/{id}/review")
    public CardResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return flashcards.review(id, request.grade());
    }
}
