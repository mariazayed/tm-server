package trademind.tm_server.invest.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import trademind.tm_server.ai.LlmResult;
import trademind.tm_server.exceptions.BadRequestException;

import java.util.function.UnaryOperator;

/**
 * Runs a model call whose answer must be JSON in a fixed shape, for the features that return
 * suggestions rather than prose (the rule check, the tag suggestions, the check-in summary).
 *
 * <p>The reply is parsed with {@link ModelJson}. If it cannot be used, the call is repeated <b>once</b>
 * with a short note saying what was wrong, and if the second reply is no better the caller gets an
 * {@link AiInvalidReplyException}. Unparsed model text is never returned. Both calls are real calls: each
 * counts against the daily and per-minute caps, and a cap reached on the repeat surfaces as the usual
 * 429. A failed call (network, provider error) is not repeated here: that is not the model's formatting.
 */
@Service
public class AiJsonService {

    private static final Logger log = LoggerFactory.getLogger(AiJsonService.class);

    /** @param repaired true when the first reply was unusable and the second one worked */
    public record Result<T>(T value, boolean repaired, long inputTokens, long outputTokens) {
    }

    private final AiService ai;

    public AiJsonService(AiService ai) {
        this.ai = ai;
    }

    /**
     * @param check runs on the parsed value: it may return a cleaned copy (for example with unknown ids and
     *              unverifiable quotes removed) or throw {@link ModelJsonException} to ask for a repeat
     * @throws AiInvalidReplyException neither reply was usable
     */
    public <T> Result<T> call(AiFeature feature, String systemPrompt, String userPrompt, Class<T> type,
                              UnaryOperator<T> check) {
        LlmResult first = ai.call(feature, systemPrompt, userPrompt);
        Attempt<T> a = attempt(first, type, check);
        if (a.value != null) {
            return new Result<>(a.value, false, tokens(first.inputTokens()), tokens(first.outputTokens()));
        }

        // The reason is built from our own field names, never from the reply, so it is safe to log and to send.
        log.info("ai json repair feature={} reason={}", feature, a.reason);
        LlmResult second;
        try {
            second = ai.call(feature, systemPrompt, userPrompt + repairNote(a.reason, first.truncated()));
        } catch (BadRequestException e) {
            throw new AiInvalidReplyException(); // the repair note pushed an already large prompt over the cap
        }
        Attempt<T> b = attempt(second, type, check);
        long in = tokens(first.inputTokens()) + tokens(second.inputTokens());
        long out = tokens(first.outputTokens()) + tokens(second.outputTokens());
        if (b.value == null) {
            log.warn("ai json unusable after repair feature={} reason={}", feature, b.reason);
            throw new AiInvalidReplyException();
        }
        return new Result<>(b.value, true, in, out);
    }

    private record Attempt<T>(T value, String reason) {
    }

    private <T> Attempt<T> attempt(LlmResult reply, Class<T> type, UnaryOperator<T> check) {
        if (reply.truncated()) {
            // Cut off at the reply limit: whatever parses from it is incomplete, so it is not trusted.
            return new Attempt<>(null, "the reply was cut off at the length limit");
        }
        try {
            T cleaned = check.apply(ModelJson.parse(reply.text(), type));
            if (cleaned == null) return new Attempt<>(null, "the reply had no usable content");
            return new Attempt<>(cleaned, null);
        } catch (ModelJsonException e) {
            return new Attempt<>(null, e.getMessage());
        }
    }

    static String repairNote(String reason, boolean wasCutOff) {
        return "\n\n## Problem with your previous reply\n"
                + "Your previous reply could not be used: " + reason + ". "
                + "Reply again with only the JSON object described above: no other text and no code fence."
                + (wasCutOff ? " Keep it shorter this time." : "")
                + "\n";
    }

    private static long tokens(Integer value) {
        return value == null ? 0 : value;
    }
}
