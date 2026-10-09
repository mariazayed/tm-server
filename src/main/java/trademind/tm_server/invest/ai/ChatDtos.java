package trademind.tm_server.invest.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Request and response shapes for {@code POST /api/v1/invest/ai/chat}. */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** @param role "user" or "assistant", any case */
    public record Message(
            @NotBlank @Pattern(regexp = "(?i)user|assistant") String role,
            @NotBlank @Size(max = ChatPromptBuilder.MAX_MESSAGE_CHARS) String text) {
    }

    /**
     * @param history        the last few turns, oldest first; the server keeps no conversation of its own
     * @param includeJournal null means true
     * @param includeNotes   null means true
     */
    public record ChatRequest(
            @NotBlank @Size(max = AiPromptAssembler.MAX_QUESTION_CHARS) String question,
            @Size(max = ChatPromptBuilder.MAX_HISTORY_MESSAGES) List<@Valid Message> history,
            Boolean includeJournal,
            Boolean includeNotes) {

        public boolean journal() {
            return includeJournal == null || includeJournal;
        }

        public boolean notes() {
            return includeNotes == null || includeNotes;
        }

        public List<Message> turns() {
            return history == null ? List.of() : history;
        }
    }

    /** @param type JOURNAL or NOTE; {@code snippet} is the text of the owner's entry that was shown to the model */
    public record Source(String handle, String type, long id, LocalDate date, String snippet) {
    }

    /**
     * @param sources   only sources the model cited and that were really in the prompt
     * @param truncated something was left out of the prompt to fit the size budget
     * @param cutOff    the model hit its reply limit, so the answer may end early
     * @param refusal   null normally; ADVICE when the answer was replaced because it gave advice, EMPTY when none came
     */
    public record ChatResponse(String answer, List<Source> sources, boolean usedJournal, boolean usedNotes,
                               boolean truncated, boolean cutOff, String model, String refusal) {
    }
}
