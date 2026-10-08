package trademind.tm_server.invest.ai;

import java.util.List;

/**
 * The JSON shape the model must reply with for the consistency check. Every key is required (see
 * {@link ModelJson}); {@code quote} may be null. The values are raw model output until
 * {@link RuleCheckAiService} has checked them against what was really sent.
 */
record RuleCheckModelReply(List<Finding> findings) {

    enum Confidence { LOW, MEDIUM, HIGH }

    record Finding(String rule, List<String> entries, String reason, String quote, Confidence confidence) {
    }
}
