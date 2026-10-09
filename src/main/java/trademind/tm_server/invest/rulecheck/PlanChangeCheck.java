package trademind.tm_server.invest.rulecheck;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Never change the plan without writing the reason in the journal first." A change to the monthly
 * amount, the ticker or the buy day is a finding if there is no journal entry of any kind in the
 * {@value #WINDOW_DAYS} days before it. Fields saved together are one change. This is deliberately
 * lenient (any entry counts, so there are no false alarms); it cannot tell whether the entry is the reason.
 */
final class PlanChangeCheck implements RuleCheck {

    static final String CODE = "PLAN_CHANGE";
    static final int WINDOW_DAYS = 7;
    /** Fields written by one save are stamped within moments of each other. */
    private static final Duration SAME_SAVE = Duration.ofSeconds(5);

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "Plan changed without a note";
    }

    @Override
    public List<RuleFinding> run(Facts facts) {
        List<RuleFinding> out = new ArrayList<>();
        for (List<Facts.PlanChange> save : bySave(facts.changes())) {
            LocalDateTime at = save.get(0).at();
            if (!facts.inRange(at.toLocalDate())) continue;
            boolean noted = facts.entries().stream().anyMatch(e ->
                    !e.createdAt().isAfter(at) && !e.createdAt().isBefore(at.minusDays(WINDOW_DAYS)));
            if (noted) continue;
            List<String> parts = save.stream().map(c -> describe(c, facts.currency())).toList();
            out.add(RuleFinding.deterministic(CODE, at.toLocalDate(),
                    "The plan was changed on " + at.toLocalDate() + " (" + String.join("; ", parts) + "), with no "
                            + "journal entry in the " + WINDOW_DAYS + " days before it. Your rule says to write the "
                            + "reason first.",
                    List.of()));
        }
        return out;
    }

    private static List<List<Facts.PlanChange>> bySave(List<Facts.PlanChange> changes) {
        List<Facts.PlanChange> sorted = changes.stream().sorted(Comparator.comparing(Facts.PlanChange::at)).toList();
        List<List<Facts.PlanChange>> saves = new ArrayList<>();
        for (Facts.PlanChange c : sorted) {
            if (!saves.isEmpty()) {
                List<Facts.PlanChange> last = saves.get(saves.size() - 1);
                if (Duration.between(last.get(0).at(), c.at()).abs().compareTo(SAME_SAVE) <= 0) {
                    last.add(c);
                    continue;
                }
            }
            saves.add(new ArrayList<>(List.of(c)));
        }
        return saves;
    }

    private static String describe(Facts.PlanChange c, String currency) {
        return switch (c.field()) {
            case "MONTHLY_AMOUNT" -> "monthly amount " + money(c.oldValue(), currency) + " to " + money(c.newValue(), currency);
            case "TICKER" -> "ticker " + c.oldValue() + " to " + c.newValue();
            case "BUY_DAY" -> "buy day " + c.oldValue() + " to " + c.newValue();
            default -> c.field().toLowerCase(java.util.Locale.ROOT) + " changed";
        };
    }

    private static String money(String cents, String currency) {
        try {
            return new BigDecimal(cents).movePointLeft(2).setScale(2).toPlainString() + " " + currency;
        } catch (NumberFormatException e) {
            return String.valueOf(cents);
        }
    }
}
