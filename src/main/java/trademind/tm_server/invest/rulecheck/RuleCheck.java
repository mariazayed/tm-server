package trademind.tm_server.invest.rulecheck;

import java.util.List;

/** One deterministic check: dates and settings in, findings out. No model, no database, no side effects. */
interface RuleCheck {

    /** Stable code shown in the app and used in {@link RuleFinding#check()}. */
    String code();

    /** Short name for the list of checks that ran. */
    String label();

    /** Null when the check runs; otherwise why it is off (for example a setting of 0). */
    default String offReason(Facts facts) {
        return null;
    }

    List<RuleFinding> run(Facts facts);
}
