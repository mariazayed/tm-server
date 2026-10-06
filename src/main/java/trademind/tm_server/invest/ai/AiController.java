package trademind.tm_server.invest.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

import java.time.Instant;

/** Under /api/v1/invest, so a login token is required. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai")
public class AiController {

    /** Whether the server could call a model. The key itself is never returned. */
    public record Status(boolean configured, String model, String reason) {
    }

    /** What an AI feature would be shown, exactly as it would be sent. */
    public record ContextPreview(Instant generatedAt, boolean includeJournal, boolean includeNotes, boolean truncated,
                                 int characters, int maxCharacters, String text) {
    }

    private final UserContextService contextService;
    private final String apiKey;
    private final String model;

    public AiController(UserContextService contextService,
                        @Value("${ai.provider.api-key:}") String apiKey,
                        @Value("${ai.provider.model:}") String model) {
        this.contextService = contextService;
        this.apiKey = apiKey;
        this.model = model;
    }

    @GetMapping("/status")
    public Status status() {
        return status(apiKey, model);
    }

    static Status status(String apiKey, String model) {
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        boolean hasModel = model != null && !model.isBlank();
        if (hasKey && hasModel) return new Status(true, model.strip(), null);
        if (!hasKey && !hasModel) return new Status(false, null, "Set ai.provider.api-key and ai.provider.model on the server");
        return new Status(false, hasModel ? model.strip() : null,
                hasKey ? "Set ai.provider.model on the server" : "Set ai.provider.api-key on the server");
    }

    @GetMapping("/context")
    public ContextPreview context(@RequestParam(defaultValue = "true") boolean includeJournal,
                                  @RequestParam(defaultValue = "true") boolean includeNotes) {
        UserContext context = contextService.build(new ContextOptions(includeJournal, includeNotes));
        UserContextRenderer.Rendered rendered = UserContextRenderer.render(context);
        return new ContextPreview(context.generatedAt(), includeJournal, includeNotes, rendered.truncated(),
                rendered.text().length(), UserContextRenderer.MAX_CHARS, rendered.text());
    }
}
