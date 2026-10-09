package trademind.tm_server.invest.checkin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.invest.ai.CheckinSummaryAiService;

/**
 * The facts of a month, and optionally a drafted summary of them. The facts are plain arithmetic and are
 * always returned, with AI off or on. The draft is added only when asked for ({@code includeModel}), because it
 * sends the facts and, unless {@code includeJournal} is false, the month's journal text to the AI provider.
 * Read-only: nothing is saved; the draft is text for the owner to edit and put in a check-in entry.
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai/checkin-summary")
public class CheckinSummaryController {

    /**
     * @param month          YYYY-MM
     * @param includeModel   true also asks for a drafted summary; off unless asked for
     * @param includeJournal false keeps the journal text out of the draft's prompt; defaults to true
     */
    public record CheckinSummaryRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String month, Boolean includeModel,
                                        Boolean includeJournal) {
    }

    private final CheckinSummaryAiService service;

    public CheckinSummaryController(CheckinSummaryAiService service) {
        this.service = service;
    }

    @PostMapping
    public CheckinSummaryResponse summarize(@Valid @RequestBody CheckinSummaryRequest request) {
        boolean model = Boolean.TRUE.equals(request.includeModel());
        boolean journal = request.includeJournal() == null || request.includeJournal();
        return service.summarize(request.month(), model, journal);
    }
}
