package trademind.tm_server.invest.rulecheck;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

import java.time.LocalDate;

/**
 * Consistency checks against the owner's own rules. It lives under {@code /ai} because the model-based
 * check joins it later, but the checks here are plain code: this endpoint works with AI switched off and
 * never calls a model. Read-only.
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai/rule-check")
public class RuleCheckController {

    public record RuleCheckRequest(@NotNull LocalDate from, @NotNull LocalDate to) {
    }

    private final RuleCheckService service;

    public RuleCheckController(RuleCheckService service) {
        this.service = service;
    }

    @PostMapping
    public RuleCheckService.Result check(@Valid @RequestBody RuleCheckRequest request) {
        return service.run(request.from(), request.to());
    }
}
