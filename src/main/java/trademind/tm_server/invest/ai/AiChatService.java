package trademind.tm_server.invest.ai;

import trademind.tm_server.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import trademind.tm_server.ai.LlmResult;
import trademind.tm_server.invest.ai.ChatDtos.ChatRequest;
import trademind.tm_server.invest.ai.ChatDtos.ChatResponse;
import trademind.tm_server.invest.ai.ChatDtos.Message;
import trademind.tm_server.invest.ai.ChatDtos.Source;

import java.util.List;

/**
 * "Ask your journal": answers a question from the owner's rules, numbers, journal and notes. Read-only:
 * it reads through {@link UserContextService} and {@link RetrievalService}, calls the model through
 * {@link AiService}, and writes nothing. The server keeps no conversation; the client sends the last few
 * turns each time.
 */
@Service
public class AiChatService {

    private final AiService ai;
    private final RetrievalService retrieval;
    private final UserContextService context;
    private final AiProperties properties;

    public AiChatService(AiService ai, RetrievalService retrieval, UserContextService context, AiProperties properties) {
        this.ai = ai;
        this.retrieval = retrieval;
        this.context = context;
        this.properties = properties;
    }

    public ChatResponse chat(ChatRequest request) {
        ai.requireAvailable(); // before any database work
        ChatPromptBuilder.Built built = buildPrompt(request.question(), request.turns(), request.journal(), request.notes());
        LlmResult result = ai.call(AiFeature.CHAT, built.prompt().system(), built.prompt().user());

        ChatAnswerCheck.Checked checked = ChatAnswerCheck.check(result.text(), built.sources().keySet());
        List<Source> sources = checked.handles().stream().map(h -> toSource(built.sources().get(h))).toList();
        return new ChatResponse(checked.answer(), sources, request.journal(), request.notes(), built.truncated(),
                result.truncated(), ai.status().model(), checked.refusal());
    }

    /**
     * The exact prompt a chat call would send. Used by {@link #chat} and by the context preview, so the
     * two cannot drift apart.
     */
    ChatPromptBuilder.Built buildPrompt(String question, List<Message> history, boolean includeJournal,
                                        boolean includeNotes) {
        if (question == null || question.isBlank()) throw new BadRequestException("Ask a question first");
        List<ChatPromptBuilder.Turn> turns = history.stream()
                .map(m -> new ChatPromptBuilder.Turn("assistant".equalsIgnoreCase(m.role()), m.text())).toList();
        ChatPromptBuilder.validateHistory(turns);

        // The free text comes from retrieval, so the standing context carries numbers and rules only.
        UserContextRenderer.Rendered base = UserContextRenderer.render(context.build(new ContextOptions(false, false)));
        RetrievalService.Result retrieved = retrieval.search(question, includeJournal, includeNotes);
        return ChatPromptBuilder.build(base, retrieved, includeJournal, includeNotes, turns, question,
                properties.limits().maxPromptChars());
    }

    private static Source toSource(RetrievalService.Snippet s) {
        return new Source(s.handle(), s.kind().name(), s.id(), s.date(), s.text());
    }
}
