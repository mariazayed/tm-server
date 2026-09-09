package trademind.tm_server.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Exercises the pure parsing logic directly (no Spring context, no network
// call) -- same testing philosophy as TradeEnrichmentServiceTest. These
// specific cases (fenced JSON, prose-wrapped JSON, garbage text, unknown
// extra keys) were run for real against this exact class during
// development using a standalone Jackson classpath, since a live LLM call
// can't be exercised from the build environment -- this file reproduces
// those same cases as the project's checked-in regression tests.
class AiSuggestionResponseParserTest {

    @Test
    void parsesCleanJson() {
        AiSuggestion result = AiSuggestionResponseParser.parse(
                "{\"entryReason\":\"BREAKOUT\",\"marketState\":\"UPTREND\",\"marketBias\":\"BULLISH\"}"
        );
        assertEquals("BREAKOUT", result.entryReason());
        assertEquals("UPTREND", result.marketState());
        assertEquals("BULLISH", result.marketBias());
        assertNull(result.probability());
    }

    @Test
    void stripsMarkdownCodeFence() {
        AiSuggestion result = AiSuggestionResponseParser.parse("```json\n{\"entryReason\":\"RETEST\"}\n```");
        assertEquals("RETEST", result.entryReason());
    }

    @Test
    void extractsJsonFromSurroundingProse() {
        AiSuggestion result = AiSuggestionResponseParser.parse(
                "Sure, here is my analysis:\n{\"probability\":\"LOW\"}\nLet me know if you need more."
        );
        assertEquals("LOW", result.probability());
    }

    @Test
    void garbageTextYieldsEmptyRatherThanThrowing() {
        AiSuggestion result = AiSuggestionResponseParser.parse("I cannot determine this from the given data.");
        assertEquals(AiSuggestion.empty(), result);
    }

    @Test
    void blankOrNullInputYieldsEmpty() {
        assertEquals(AiSuggestion.empty(), AiSuggestionResponseParser.parse(""));
        assertEquals(AiSuggestion.empty(), AiSuggestionResponseParser.parse(null));
    }

    @Test
    void unknownExtraKeysAreTolerated() {
        AiSuggestion result = AiSuggestionResponseParser.parse(
                "{\"entryReason\":\"PULLBACK\",\"rationale\":\"price pulled back into a discount OB\"}"
        );
        assertEquals("PULLBACK", result.entryReason());
    }
}
