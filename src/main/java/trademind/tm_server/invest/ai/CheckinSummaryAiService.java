package trademind.tm_server.invest.ai;

import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.checkin.CheckinFacts;
import trademind.tm_server.invest.checkin.CheckinFactsRenderer;
import trademind.tm_server.invest.checkin.CheckinFactsService;
import trademind.tm_server.invest.checkin.CheckinSummaryResponse;
import trademind.tm_server.invest.checkin.CheckinSummaryResponse.ModelInfo;

import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The monthly check-in summary: code-computed facts, and optionally a drafted summary of them. The facts
 * are always returned. The draft is added only when asked for, is suggestion-only (it is text for the
 * owner to edit and save themselves) and is checked before it is shown: no number in it that is not in the
 * facts or the owner's own entries, no length beyond what was asked, no advice. A draft that fails gets
 * one repeat, and if that fails too the facts are returned with the reason. Read-only.
 */
@Service
public class CheckinSummaryAiService {

    static final int MAX_WORDS = 220; // 180 asked for, with some slack
    static final int MAX_NUMBERS_IN_REASON = 3;

    private final CheckinFactsService facts;
    private final AiJsonService json;
    private final AiService ai;
    private final AiProperties properties;

    public CheckinSummaryAiService(CheckinFactsService facts, AiJsonService json, AiService ai, AiProperties properties) {
        this.facts = facts;
        this.json = json;
        this.ai = ai;
        this.properties = properties;
    }

    public CheckinSummaryResponse summarize(String monthText, boolean includeModel, boolean includeJournal) {
        YearMonth month = facts.parse(monthText);
        CheckinFactsService.Loaded loaded = facts.load(month);
        List<String> lines = CheckinFactsRenderer.lines(loaded.facts());
        if (!includeModel) return response(loaded.facts(), lines, null, List.of(), ModelInfo.notRequested());

        try {
            ai.requireAvailable();
            CheckinPromptBuilder.Built built = CheckinPromptBuilder.build(month.toString(), lines, loaded.entries(),
                    includeJournal, properties.limits().maxPromptChars());
            AiJsonService.Result<CheckinSummaryReply> result = json.call(AiFeature.CHECKIN_SUMMARY,
                    built.prompt().system(), built.prompt().user(), CheckinSummaryReply.class, reply -> clean(reply, built));
            List<Long> sources = result.value().sources().stream().map(h -> built.entries().get(h).entry().id()).toList();
            return response(loaded.facts(), lines, result.value().text(), sources,
                    new ModelInfo(true, true, null, built.entries().size(), built.skipped()));
        } catch (AiUnavailableException | AiLimitExceededException e) {
            return response(loaded.facts(), lines, null, List.of(), ModelInfo.notRun(e.getMessage())); // fixed, safe wording
        } catch (AiUpstreamException e) {
            return response(loaded.facts(), lines, null, List.of(),
                    ModelInfo.notRun("The AI service did not answer. The numbers above are still correct."));
        } catch (AiInvalidReplyException e) {
            return response(loaded.facts(), lines, null, List.of(),
                    ModelInfo.notRun("The AI's draft could not be used (it kept getting something wrong). The numbers above are still correct."));
        } catch (BadRequestException e) {
            return response(loaded.facts(), lines, null, List.of(),
                    ModelInfo.notRun("There is too much text for one summary. Try again without the journal."));
        }
    }

    /** The exact prompt a draft would send, for the "what will be sent" preview. */
    CheckinPromptBuilder.Built buildPrompt(String monthText, boolean includeJournal) {
        YearMonth month = facts.parse(monthText);
        CheckinFactsService.Loaded loaded = facts.load(month);
        return CheckinPromptBuilder.build(month.toString(), CheckinFactsRenderer.lines(loaded.facts()), loaded.entries(),
                includeJournal, properties.limits().maxPromptChars());
    }

    /**
     * Checks the model's draft before it can be shown. Anything wrong asks for a repeat with a reason that
     * contains no model text (numbers are digits only).
     */
    static CheckinSummaryReply clean(CheckinSummaryReply reply, CheckinPromptBuilder.Built built) {
        String text = reply.text() == null ? "" : reply.text().replaceAll("\\[[JR]\\d+]", "").strip();
        if (text.isEmpty()) throw new ModelJsonException("the text was empty");
        if (text.split("\\s+").length > MAX_WORDS) throw new ModelJsonException("the text was longer than the 180 words asked for");
        if (ChatAnswerCheck.givesAdvice(text)) throw new ModelJsonException("the text told the owner what to buy, sell or hold");
        List<String> unknown = NumberCheck.unknown(text, built.numberSources());
        if (!unknown.isEmpty()) {
            throw new ModelJsonException("the text used numbers that are not in the Facts: "
                    + String.join(", ", unknown.subList(0, Math.min(unknown.size(), MAX_NUMBERS_IN_REASON)))
                    + ". Copy numbers exactly from the Facts or leave them out");
        }
        Set<String> handles = new LinkedHashSet<>();
        if (reply.sources() != null) {
            for (String raw : reply.sources()) {
                String h = raw == null ? "" : raw.strip().replace("[", "").replace("]", "").toUpperCase(java.util.Locale.ROOT);
                if (built.entries().containsKey(h)) handles.add(h);
            }
        }
        return new CheckinSummaryReply(text, List.copyOf(handles));
    }

    private static CheckinSummaryResponse response(CheckinFacts f, List<String> lines, String text, List<Long> sources, ModelInfo model) {
        return new CheckinSummaryResponse(CheckinSummaryResponse.KIND, f.month(), text, f, lines, sources, f.contributionId(), model);
    }
}
