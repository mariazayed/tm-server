package trademind.tm_server.invest.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks on a chat reply, applied after the model has answered. They are a safety net on
 * top of the system prompt, not a replacement for it:
 *
 * <ul>
 *   <li><b>Citations.</b> A handle such as {@code [J12]} that was not in the prompt is removed from the
 *       text, so an invented source never reaches the screen. The valid ones become the reply's sources.
 *   <li><b>Advice.</b> A sentence that tells the owner to buy, sell, hold, change the plan, or that
 *       predicts a price or return with a number replaces the whole answer with a fixed refusal. A
 *       sentence that is clearly restating the owner's own rule or note ("your rule says you should never
 *       sell in a crash") is exempt. The patterns are short on purpose and will sometimes refuse a harmless
 *       answer; a refusal costs a retry, advice costs trust.
 * </ul>
 */
final class ChatAnswerCheck {

    static final String ADVICE = "ADVICE";
    static final String EMPTY = "EMPTY";

    static final String ADVICE_REFUSAL = "I can't tell you what to buy, sell or hold, and I can't predict prices or "
            + "returns. I'm not a financial adviser. Your own written rules are the best guide, and you can read them "
            + "on the Rules screen. You can ask me what you wrote about a situation, or how consistent you have been.";
    static final String EMPTY_REPLY = "I could not produce an answer. Try asking in a different way.";

    /** @param refusal null for a normal answer, otherwise {@link #ADVICE} or {@link #EMPTY} */
    record Checked(String answer, List<String> handles, String refusal) {
    }

    private static final Pattern CITATION = Pattern.compile("\\[\\s*([JN]\\d+(?:\\s*[,;]\\s*[JN]\\d+)*)\\s*]");
    private static final Pattern HANDLE = Pattern.compile("[JN]\\d+");
    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?])\\s+|\\n+");

    /** Words that show the sentence is reporting what the owner wrote rather than advising. */
    private static final Pattern ATTRIBUTION = Pattern.compile(
            "\\b(rule|rules|wrote|written|writes|said|says|noted|your own)\\b", Pattern.CASE_INSENSITIVE);

    /** An action on the portfolio or the plan, in any common form ("pause", "pausing", "increased"). */
    private static final String ACTION = "(?:buy(?:ing)?|sell(?:ing)?|hold(?:ing)?|increas\\w*|decreas\\w*|reduc\\w*|rais\\w*"
            + "|switch\\w*|paus\\w*|skip\\w*|stop(?:ping)? investing|tim(?:e|ing) the market)";
    private static final List<Pattern> ADVICE_PATTERNS = List.of(
            // "you should sell", "you must buy more", "you might want to hold"
            Pattern.compile("\\byou(?:'d| should| must| ought to| need to| have to| had better| might want to| could consider)\\s+"
                    + "(?:\\w+\\s+){0,2}?" + ACTION + "\\b", Pattern.CASE_INSENSITIVE),
            // "I recommend buying", "I'd suggest you sell"
            Pattern.compile("\\bI(?:'d| would)? (?:recommend|suggest|advise|urge)\\b[^.!?\\n]{0,40}?\\b"
                    + "(?:buy|sell|hold|increas|decreas|reduc|rais|switch|paus|skip|stop)\\w*", Pattern.CASE_INSENSITIVE),
            // "it's a good time to buy", "now is the time to sell"
            Pattern.compile("\\b(?:good|great|right|best|perfect) time to\\s+" + ACTION + "\\b", Pattern.CASE_INSENSITIVE),
            // "the price will reach 150", "VT is going to rise 20%"
            Pattern.compile("\\b(?:will|going to|likely to)\\s+(?:\\w+\\s+){0,3}?"
                    + "(?:reach|rise|fall|drop|climb|rally|double|return|gain)\\b[^.!?\\n]{0,40}\\d", Pattern.CASE_INSENSITIVE),
            // "expect returns of 8%"
            Pattern.compile("\\bexpect(?:ing)?\\s+(?:\\w+\\s+){0,3}?(?:returns?|gains?|growth)\\b[^.!?\\n]{0,40}\\d",
                    Pattern.CASE_INSENSITIVE));

    private ChatAnswerCheck() {
    }

    /** @param validHandles the handles that were in the prompt, such as {@code J12} and {@code N7} */
    static Checked check(String raw, Set<String> validHandles) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) return new Checked(EMPTY_REPLY, List.of(), EMPTY);

        Set<String> cited = new LinkedHashSet<>();
        String cleaned = tidy(replaceCitations(text, validHandles, cited));
        if (cleaned.isEmpty()) return new Checked(EMPTY_REPLY, List.of(), EMPTY);
        if (givesAdvice(cleaned)) return new Checked(ADVICE_REFUSAL, List.of(), ADVICE);
        return new Checked(cleaned, List.copyOf(cited), null);
    }

    /** Keeps valid handles (rewritten as one bracket each) and drops the rest. */
    private static String replaceCitations(String text, Set<String> valid, Set<String> cited) {
        Matcher m = CITATION.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            List<String> keep = new ArrayList<>();
            Matcher h = HANDLE.matcher(m.group(1));
            while (h.find()) {
                if (valid.contains(h.group()) && !keep.contains(h.group())) keep.add(h.group());
            }
            cited.addAll(keep);
            StringBuilder replacement = new StringBuilder();
            for (String handle : keep) replacement.append('[').append(handle).append(']');
            m.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Closes the gaps a removed citation leaves behind. */
    private static String tidy(String text) {
        return text.replaceAll("[ \\t]{2,}", " ").replaceAll(" +([.,;:!?])", "$1").replaceAll(" +\\n", "\n").strip();
    }

    static boolean givesAdvice(String text) {
        for (String sentence : SENTENCE_BREAK.split(text)) {
            if (ATTRIBUTION.matcher(sentence).find()) continue;
            for (Pattern p : ADVICE_PATTERNS) {
                if (p.matcher(sentence).find()) return true;
            }
        }
        return false;
    }
}
