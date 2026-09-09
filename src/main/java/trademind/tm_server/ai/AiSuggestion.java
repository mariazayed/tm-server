package trademind.tm_server.ai;

/**
 * Raw, untyped result of parsing the LLM's JSON response -- every field is
 * just the string the model returned (or null), NOT yet validated against
 * its target enum. AiSuggestionService does that validation (via a
 * case-insensitive Enum.valueOf lookup that falls back to null on any
 * mismatch) at the point of applying each field, so a hallucinated or
 * misspelled value degrades to "no suggestion" rather than throwing.
 */
public record AiSuggestion(
        String entryReason,
        String marketState,
        String invalidationCondition,
        String probability,
        String entryZoneType,
        String entryConfirmation,
        String exitReason,
        String slPlacementReason,
        String tpPlacementReason,
        String marketBias
) {
    public static AiSuggestion empty() {
        return new AiSuggestion(null, null, null, null, null, null, null, null, null, null);
    }
}
