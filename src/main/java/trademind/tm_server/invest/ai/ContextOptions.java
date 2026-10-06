package trademind.tm_server.invest.ai;

/**
 * Which free-text parts of the owner's data may be included. Numbers and rules are always included;
 * the journal and notes are the personal parts, so they can be switched off per request.
 */
public record ContextOptions(boolean includeJournal, boolean includeNotes) {

    public static ContextOptions all() {
        return new ContextOptions(true, true);
    }
}
