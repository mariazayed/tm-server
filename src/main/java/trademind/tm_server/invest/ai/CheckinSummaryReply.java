package trademind.tm_server.invest.ai;

import java.util.List;

/**
 * The JSON the model must reply with for the check-in summary: the text, and the handles of the journal
 * entries it drew on (an empty list if none). Raw model output until {@link CheckinSummaryAiService} has
 * checked it.
 */
record CheckinSummaryReply(String text, List<String> sources) {
}
