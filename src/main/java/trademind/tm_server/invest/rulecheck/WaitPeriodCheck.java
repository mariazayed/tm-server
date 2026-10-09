package trademind.tm_server.invest.rulecheck;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Wait N hours before any deviation." The convention is two kinds of journal entry: a {@code decision}
 * entry when the owner decides to deviate from the plan, and a {@code deviation} entry when they act. For
 * each deviation entry in the range the check finds the <b>earliest</b> decision entry in the
 * {@value #LOOKBACK_DAYS} days before it (older decisions are treated as unrelated) and compares the gap
 * with the owner's wait. No such decision entry, or a gap shorter than the wait, is a finding.
 *
 * <p>An entry tagged both is a deviation, not a decision. The rule's text is never read: the wait comes
 * from the setting (0 turns the check off).
 */
final class WaitPeriodCheck implements RuleCheck {

    static final String CODE = "WAIT_PERIOD";
    static final int LOOKBACK_DAYS = 14;
    static final String DEVIATION = "deviation";
    static final String DECISION = "decision";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "Wait before a deviation";
    }

    @Override
    public String offReason(Facts facts) {
        return facts.waitHours() <= 0 ? "Switched off: the wait is set to 0 hours in Settings." : null;
    }

    @Override
    public List<RuleFinding> run(Facts facts) {
        if (offReason(facts) != null) return List.of();
        List<RuleFinding> out = new ArrayList<>();
        List<Facts.Entry> deviations = facts.entries().stream()
                .filter(e -> e.tags().contains(DEVIATION) && facts.inRange(e.createdAt().toLocalDate()))
                .sorted(Comparator.comparing(Facts.Entry::createdAt).thenComparingLong(Facts.Entry::id))
                .toList();
        for (Facts.Entry d : deviations) {
            Facts.Entry decision = facts.entries().stream()
                    .filter(e -> e.tags().contains(DECISION) && !e.tags().contains(DEVIATION))
                    .filter(e -> e.createdAt().isBefore(d.createdAt()) && !e.createdAt().isBefore(d.createdAt().minusDays(LOOKBACK_DAYS)))
                    .min(Comparator.comparing(Facts.Entry::createdAt).thenComparingLong(Facts.Entry::id))
                    .orElse(null);
            if (decision == null) {
                out.add(RuleFinding.deterministic(CODE, d.createdAt().toLocalDate(),
                        "Entry " + d.id() + " (" + d.createdAt().toLocalDate() + ") records a deviation, but there is "
                                + "no decision entry in the " + LOOKBACK_DAYS + " days before it. Your wait is "
                                + facts.waitHours() + " hours between deciding and acting.",
                        List.of(d.id())));
                continue;
            }
            long hours = Duration.between(decision.createdAt(), d.createdAt()).toHours();
            if (hours < facts.waitHours()) {
                out.add(RuleFinding.deterministic(CODE, d.createdAt().toLocalDate(),
                        "Entry " + d.id() + " (" + d.createdAt().toLocalDate() + ") records a deviation "
                                + (hours < 1 ? "less than an hour" : "only " + hours + (hours == 1 ? " hour" : " hours"))
                                + " after the decision in entry " + decision.id() + " (" + decision.createdAt().toLocalDate()
                                + "). Your wait is " + facts.waitHours() + " hours.",
                        List.of(decision.id(), d.id())));
            }
        }
        return out;
    }
}
