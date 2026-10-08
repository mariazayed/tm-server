package trademind.tm_server.invest.checkin;

import java.util.List;

/**
 * A month's facts and, if it was asked for and could be written, a drafted summary.
 *
 * @param text           the draft, or null when no draft was asked for or it could not be written
 * @param facts          the code-computed numbers; always present, and the only source of numbers in {@code text}
 * @param factsLines     {@code facts} as plain lines, exactly as shown to the model
 * @param sources        ids of the journal entries the draft drew on
 * @param contributionId the month's latest buy, to link a check-in entry to it
 * @param model          what happened with the optional AI draft
 */
public record CheckinSummaryResponse(String kind, String month, String text, CheckinFacts facts, List<String> factsLines,
                                     List<Long> sources, Long contributionId, ModelInfo model) {

    public static final String KIND = "CHECKIN_SUMMARY";

    /**
     * @param requested     a draft was asked for
     * @param ran           the model was called and its draft passed every check
     * @param reason        when requested but not written: why (AI off, a limit, an unusable reply twice, ...)
     * @param entriesSent   journal entries the model read
     * @param entriesSkipped entries of the month it did not read
     */
    public record ModelInfo(boolean requested, boolean ran, String reason, int entriesSent, int entriesSkipped) {

        public static ModelInfo notRequested() {
            return new ModelInfo(false, false, null, 0, 0);
        }

        public static ModelInfo notRun(String reason) {
            return new ModelInfo(true, false, reason, 0, 0);
        }
    }
}
