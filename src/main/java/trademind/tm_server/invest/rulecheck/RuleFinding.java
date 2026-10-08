package trademind.tm_server.invest.rulecheck;

import java.time.LocalDate;
import java.util.List;

/**
 * One thing that may not be consistent with the owner's own rules. A finding is a prompt to look, never
 * a verdict, and nothing stores it or marks an entry.
 *
 * @param check      which check found it, such as WAIT_PERIOD
 * @param source     DETERMINISTIC (computed from dates and settings, no model)
 * @param date       when it happened, for ordering
 * @param entryIds   the journal entries that are the evidence (may be empty)
 * @param ruleId     the owner's rule it relates to, when known; null for deterministic findings, because
 *                   rule text is free text and is never parsed
 * @param quote      a verified quotation from the owner's text; null for deterministic findings
 * @param confidence always HIGH for deterministic findings
 */
public record RuleFinding(String check, String source, LocalDate date, String message, List<Long> entryIds,
                          Long ruleId, String quote, String confidence) {

    public static final String DETERMINISTIC = "DETERMINISTIC";
    public static final String HIGH = "HIGH";

    static RuleFinding deterministic(String check, LocalDate date, String message, List<Long> entryIds) {
        return new RuleFinding(check, DETERMINISTIC, date, message, List.copyOf(entryIds), null, null, HIGH);
    }
}
