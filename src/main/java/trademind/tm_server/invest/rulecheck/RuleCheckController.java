package trademind.tm_server.invest.rulecheck;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.invest.ai.RuleCheckAiService;

import java.time.LocalDate;

/**
 * Consistency checks against the owner's own rules. The exact checks are plain code and always run, so this
 * works with AI switched off. The model's reading of the journal is added only when the request asks for it
 * ({@code includeModel}); if it cannot run, the exact findings are still returned with the reason. Read-only.
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai/rule-check")
public class RuleCheckController {

    /** @param includeModel true also asks the model to read the journal in the range; off unless asked for */
    public record RuleCheckRequest(@NotNull LocalDate from, @NotNull LocalDate to, Boolean includeModel) {
    }

    private final RuleCheckService service;
    private final RuleCheckAiService aiService;

    public RuleCheckController(RuleCheckService service, RuleCheckAiService aiService) {
        this.service = service;
        this.aiService = aiService;
    }

    @PostMapping
    public RuleCheckResponse check(@Valid @RequestBody RuleCheckRequest request) {
        if (Boolean.TRUE.equals(request.includeModel())) {
            return aiService.run(request.from(), request.to());
        }
        return RuleCheckResponse.of(service.run(request.from(), request.to()), RuleCheckResponse.ModelInfo.notRequested());
    }
}
