package trademind.tm_server.invest.rulecheck;

import java.time.LocalDate;
import java.util.List;

/**
 * The answer to a consistency check: the exact findings, optionally the model's, and what ran.
 *
 * @param findings newest first; deterministic and model findings together, told apart by {@code source}
 * @param checks   the exact checks, each RAN or OFF with the reason
 * @param model    what happened with the optional model review
 */
public record RuleCheckResponse(LocalDate from, LocalDate to, int waitHours, List<RuleFinding> findings,
                                List<RuleCheckService.CheckInfo> checks, ModelInfo model) {

    /**
     * @param requested      the caller asked for the model review
     * @param ran            the model was called and its answer used
     * @param reason         when requested but not run: why (switched off, limit reached, nothing to check, ...)
     * @param entriesChecked journal entries the model read
     * @param entriesSkipped entries in the range it did not read (only the newest are sent, within a size budget)
     * @param dropped        findings the model proposed that were discarded because they named a rule, entry or
     *                       text that was not really there, or gave advice
     */
    public record ModelInfo(boolean requested, boolean ran, String reason, int entriesChecked, int entriesSkipped,
                            int dropped) {

        public static ModelInfo notRequested() {
            return new ModelInfo(false, false, null, 0, 0, 0);
        }

        public static ModelInfo notRun(String reason) {
            return new ModelInfo(true, false, reason, 0, 0, 0);
        }
    }

    public static RuleCheckResponse of(RuleCheckService.Result exact, ModelInfo model) {
        return new RuleCheckResponse(exact.from(), exact.to(), exact.waitHours(), exact.findings(), exact.checks(), model);
    }
}
