package trademind.tm_server.invest.ai;

/**
 * Puts the standing rules, the owner's context and one question together. It only builds text; the
 * call to the model (and the API key) live behind {@code LlmClient}, on the server, so nothing here
 * can leak a credential to the client.
 */
public final class AiPromptAssembler {

    public static final int MAX_QUESTION_CHARS = 2_000;

    private AiPromptAssembler() {
    }

    public record Prompt(String system, String user) {
    }

    public static Prompt assemble(String question, UserContextRenderer.Rendered context) {
        String q = question == null ? "" : question.strip();
        if (q.isEmpty()) {
            throw new IllegalArgumentException("Ask a question first");
        }
        if (q.length() > MAX_QUESTION_CHARS) {
            throw new IllegalArgumentException("The question is too long (max " + MAX_QUESTION_CHARS + " characters)");
        }
        String user = context.text() + "\n## Question\n" + UserContextRenderer.wrap(q) + "\n";
        return new Prompt(AiGuardrails.SYSTEM_PROMPT, user);
    }
}
