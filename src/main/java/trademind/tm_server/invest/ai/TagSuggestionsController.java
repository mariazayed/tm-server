package trademind.tm_server.invest.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.invest.journal.JournalService;

import java.util.List;

/**
 * Suggests tags for a journal entry draft. Sends the draft text and the names of the owner's tags to the AI
 * provider, so the app only calls it when the owner presses the button. Read-only: it creates no tag and
 * touches no entry.
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai/tag-suggestions")
public class TagSuggestionsController {

    /**
     * @param body       the entry text, saved or not
     * @param existingTags the tags the draft already has
     */
    public record TagSuggestionsRequest(@NotBlank @Size(max = 4000) String body, @Min(1) @Max(5) Integer mood,
                                        JournalEntryType entryType,
                                        @Size(max = JournalService.MAX_TAGS) List<@NotBlank @Size(max = 64) String> existingTags) {
    }

    private final TagSuggestionsService service;

    public TagSuggestionsController(TagSuggestionsService service) {
        this.service = service;
    }

    @PostMapping
    public TagSuggestionsService.Response suggest(@Valid @RequestBody TagSuggestionsRequest request) {
        return service.suggest(request.body(), request.entryType() == null ? null : request.entryType().name(),
                request.mood(), request.existingTags());
    }
}
