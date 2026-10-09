package trademind.tm_server.invest.ai;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Checks that a quotation the model attributes to the owner is really in the owner's text. A quote that
 * is not is dropped by the caller, while the finding it supported is kept without it: a made-up quote
 * would put words in the owner's mouth.
 *
 * <p>The comparison forgives what models change without meaning to: letter case, runs of whitespace,
 * curly versus straight quotes and apostrophes, and dash styles. It does not forgive different words. A
 * quote may leave things out with {@code ...} or {@code …}; each piece must then appear in the same
 * source, in order.
 */
public final class QuoteCheck {

    /** Shorter than this proves nothing ("a", "the"), so it never counts as verified. */
    public static final int MIN_CHARS = 4;
    public static final int MAX_CHARS = 400;

    private QuoteCheck() {
    }

    /** The quote, trimmed, if it appears in one of the sources; otherwise empty. */
    public static Optional<String> verified(String quote, Collection<String> sources) {
        if (quote == null || sources == null) return Optional.empty();
        String trimmed = quote.strip();
        if (trimmed.length() > MAX_CHARS) return Optional.empty();

        List<String> pieces = java.util.Arrays.stream(normalize(trimmed).split("\\.\\.\\.|…"))
                .map(String::strip).filter(p -> !p.isEmpty()).toList();
        if (pieces.isEmpty() || pieces.stream().anyMatch(p -> p.length() < MIN_CHARS)) return Optional.empty();

        for (String source : sources) {
            if (source != null && containsInOrder(normalize(source), pieces)) return Optional.of(trimmed);
        }
        return Optional.empty();
    }

    private static boolean containsInOrder(String haystack, List<String> pieces) {
        int from = 0;
        for (String piece : pieces) {
            int at = haystack.indexOf(piece, from);
            if (at < 0) return false;
            from = at + piece.length();
        }
        return true;
    }

    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace('‘', '\'').replace('’', '\'').replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }
}
