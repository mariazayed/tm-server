package trademind.tm_server.invest.ai;

import java.util.List;

/**
 * The JSON the model must reply with for tag suggestions: up to five tags, each with a short reason. Raw
 * model output until {@link TagSuggestionsService} has checked and normalised it.
 */
record TagReply(List<Item> tags) {

    record Item(String tag, String reason) {
    }
}
