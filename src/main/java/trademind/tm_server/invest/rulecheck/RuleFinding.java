package trademind.tm_server.invest.rulecheck;

import java.time.LocalDate;
import java.util.List;

/**
 * One thing that may not be consistent with the owner's own rules. A finding is a prompt to look, never
 * a verdict, and nothing stores it or marks an entry.
 *
 * @param check      which check found it, such as WAIT_PERIOD
 * @param source     DETERMINISTIC (computed from dates and settings, no model) or MODEL (the model's reading
 *                   of the text, after its ids and quote were checked against what it was shown)
 * @param date       when it happened, for ordering
 * @param entryIds   the journal entries that are the evidence (may be empty)
 * @param ruleId     the owner's rule it relates to; set for MODEL findings, null for deterministic ones,
 *                   because rule text is free text and is never parsed
 * @param quote      a quotation from the owner's text that was checked to be really there; null when the
 *                   model gave none or it could not be verified, and for deterministic findings
 * @param confidence always HIGH for deterministic findings; LOW, MEDIUM or HIGH for MODEL findings
 */
public record RuleFinding(String check, String source, LocalDate date, String message, List<Long> entryIds,
                          Long ruleId, String quote, String confidence) {

    public static final String DETERMINISTIC = "DETERMINISTIC";
    public static final String MODEL = "MODEL";
    public static final String MODEL_CHECK = "MODEL_REVIEW";
    public static final String HIGH = "HIGH";

    static RuleFinding deterministic(String check, LocalDate date, String message, List<Long> entryIds) {
        return new RuleFinding(check, DETERMINISTIC, date, message, List.copyOf(entryIds), null, null, HIGH);
    }

    /** A finding from the model's reading. The caller has already checked the ids and the quote. */
    public static RuleFinding fromModel(LocalDate date, String message, List<Long> entryIds, Long ruleId, String quote,
                                        String confidence) {
        return new RuleFinding(MODEL_CHECK, MODEL, date, message, List.copyOf(entryIds), ruleId, quote, confidence);
    }
}
