package trademind.tm_server.invest.ai;

import trademind.tm_server.exceptions.BadRequestException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/ai")
public class AiController {

    /** Whether the server could call a model. The key itself is never returned. */
    public record Status(boolean configured, String model, String reason) {
    }

    /** What an AI feature would be shown, exactly as it would be sent. */
    /**
     * @param feature      "context" (the standing context only) or the feature previewed, such as "chat"
     * @param characters   length of {@code text}; for a feature preview, system plus text, the amount the size cap counts
     * @param systemPrompt the standing instructions sent with {@code text}; null for the plain context preview
     * @param snippets     pieces of journal or notes included (feature previews only)
     */
    public record ContextPreview(Instant generatedAt, boolean includeJournal, boolean includeNotes, boolean truncated,
                                 int characters, int maxCharacters, String text, String feature, String systemPrompt,
                                 int snippets) {
    }

    private final UserContextService contextService;
    private final AiService aiService;
    private final AiChatService chatService;
    private final RuleCheckAiService ruleCheckService;
    private final CheckinSummaryAiService checkinService;
    private final AiProperties properties;
    private final Clock clock;

    public AiController(UserContextService contextService, AiService aiService, AiChatService chatService,
                        RuleCheckAiService ruleCheckService, CheckinSummaryAiService checkinService, AiProperties properties,
                        Clock clock) {
        this.contextService = contextService;
        this.aiService = aiService;
        this.chatService = chatService;
        this.ruleCheckService = ruleCheckService;
        this.checkinService = checkinService;
        this.properties = properties;
        this.clock = clock;
    }

    /** Asks a question about the owner's own rules, journal and notes. Read-only; see {@link AiChatService}. */
    @PostMapping("/chat")
    public ChatDtos.ChatResponse chat(@Valid @RequestBody ChatDtos.ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/status")
    public Status status() {
        return aiService.status();
    }

    /** Today's calls and tokens against the caps. */
    @GetMapping("/usage")
    public AiService.Usage usage() {
        return aiService.usage();
    }

    /** Calls per day for the last {@code days} days (1 to 90), against the daily cap. */
    @GetMapping("/usage/history")
    public AiService.UsageHistory usageHistory(@RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 90) throw new BadRequestException("days must be between 1 and 90");
        return aiService.history(days);
    }

    static Status status(boolean enabled, String apiKey, String model) {
        if (!enabled) {
            boolean named = model != null && !model.isBlank();
            return new Status(false, named ? model.strip() : null, "AI is switched off. Set ai.enabled=true on the server");
        }
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

    /**
     * What would be sent. With no {@code feature} it is the standing context. With {@code feature=chat&q=...} it
     * is the complete chat prompt for that question, built by the same code the chat uses.
     */
    @GetMapping("/context")
    public ContextPreview context(@RequestParam(defaultValue = "true") boolean includeJournal,
                                  @RequestParam(defaultValue = "true") boolean includeNotes,
                                  @RequestParam(defaultValue = "context") String feature,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String month) {
        if ("chat".equals(feature)) {
            if (q == null || q.isBlank()) {
                throw new BadRequestException("Add q=<your question> to preview a chat prompt");
            }
            ChatPromptBuilder.Built built = chatService.buildPrompt(q, List.of(), includeJournal, includeNotes);
            String system = built.prompt().system();
            String text = built.prompt().user();
            return new ContextPreview(clock.instant(), includeJournal, includeNotes, built.truncated(),
                    system.length() + text.length(), properties.limits().maxPromptChars(), text, "chat", system,
                    built.sources().size());
        }
        if ("rulecheck".equals(feature)) {
            if (from == null || to == null) {
                throw new BadRequestException("Add from=<date>&to=<date> to preview the journal check");
            }
            RuleCheckPromptBuilder.Built built = ruleCheckService.buildPrompt(from, to);
            String system = built.prompt().system();
            String text = built.prompt().user();
            return new ContextPreview(clock.instant(), true, false, built.skipped() > 0,
                    system.length() + text.length(), properties.limits().maxPromptChars(), text, "rulecheck", system,
                    built.entries().size());
        }
        if ("checkin".equals(feature)) {
            if (month == null || month.isBlank()) {
                throw new BadRequestException("Add month=YYYY-MM to preview the check-in summary");
            }
            CheckinPromptBuilder.Built built = checkinService.buildPrompt(month, includeJournal);
            String system = built.prompt().system();
            String text = built.prompt().user();
            return new ContextPreview(clock.instant(), includeJournal, false, built.skipped() > 0,
                    system.length() + text.length(), properties.limits().maxPromptChars(), text, "checkin", system,
                    built.entries().size());
        }
        if (!"context".equals(feature)) {
            throw new BadRequestException("Unknown feature (use context, chat, rulecheck or checkin)");
        }
        UserContext context = contextService.build(new ContextOptions(includeJournal, includeNotes));
        UserContextRenderer.Rendered rendered = UserContextRenderer.render(context);
        return new ContextPreview(context.generatedAt(), includeJournal, includeNotes, rendered.truncated(),
                rendered.text().length(), UserContextRenderer.MAX_CHARS, rendered.text(), "context", null, 0);
    }
}
