package trademind.tm_server.invest.ai;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds numbers a model wrote that are not in the material it was given. A summary of the owner's month
 * may contain only numbers the code computed (or the owner wrote): the model words things, it does not
 * calculate. Numbers are compared by value, so {@code 300}, {@code 300.00} and {@code 300.0} are the same,
 * and {@code 05} is {@code 5} (a date's day written the other way).
 *
 * <p>Limits: numbers written as words ("three") are not seen, so the prompt asks for digits; and a number that
 * happens to equal an unrelated allowed number passes. This is a guard against invented figures, not proof
 * that every figure is used correctly.
 */
final class NumberCheck {

    /** Thousands-grouped (1,234.50) or plain (1234.5) numbers; a comma not followed by three digits ends the number. */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?");

    private NumberCheck() {
    }

    /** The numbers in {@code text} that are in none of {@code sources}, as written, without repeats. */
    static List<String> unknown(String text, Collection<String> sources) {
        Set<String> allowed = new LinkedHashSet<>();
        for (String source : sources) {
            Matcher m = NUMBER.matcher(source);
            while (m.find()) allowed.add(key(m.group()));
        }
        Set<String> unknown = new LinkedHashSet<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            if (!allowed.contains(key(m.group()))) unknown.add(m.group());
        }
        return List.copyOf(unknown);
    }

    private static String key(String raw) {
        BigDecimal value = new BigDecimal(raw.replace(",", ""));
        return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
