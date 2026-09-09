package trademind.tm_server.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns the LLM's raw text reply into an AiSuggestion. Pure and
 * Spring-free -- see AiSuggestionResponseParserTest for coverage of the
 * defensive cases (markdown-fenced JSON, garbage text, partial objects)
 * that a real model reliably produces often enough to guard against, even
 * though this specific behavior can't be exercised against a live model
 * from this build environment.
 */
public final class AiSuggestionResponseParser {

    private AiSuggestionResponseParser() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Never throws -- a response this can't make sense of (empty, garbled,
     * plain prose instead of JSON) becomes AiSuggestion.empty() rather than
     * failing the whole /suggest request, since "the model didn't produce a
     * usable answer" shouldn't look different from "the model had nothing to
     * suggest" from the caller's point of view.
     */
    public static AiSuggestion parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return AiSuggestion.empty();
        }

        String json = extractJsonObject(rawText);
        if (json == null) {
            return AiSuggestion.empty();
        }

        try {
            return MAPPER.readValue(json, AiSuggestion.class);
        } catch (Exception e) {
            return AiSuggestion.empty();
        }
    }

    /**
     * Strips a leading/trailing markdown code fence if the model wrapped its
     * JSON in one (```json ... ``` or plain ``` ... ```) despite being asked
     * not to, then trims to the outermost {...} span so any stray text
     * before/after the object doesn't break parsing.
     */
    private static String extractJsonObject(String rawText) {
        String text = rawText.trim();

        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            int lastFence = text.lastIndexOf("```");
            if (lastFence != -1) {
                text = text.substring(0, lastFence);
            }
            text = text.trim();
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
