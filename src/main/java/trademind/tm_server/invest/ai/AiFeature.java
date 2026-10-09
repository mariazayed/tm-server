package trademind.tm_server.invest.ai;

import trademind.tm_server.ai.LlmOptions;

/** The AI features, each with the reply budget and sampling it needs. Names are what usage is counted under. */
public enum AiFeature {
    CHAT(1024, null),
    CHECKIN_SUMMARY(1024, null),
    RULE_CHECK(1500, 0.2),
    TAG_SUGGESTIONS(400, 0.2);

    private final LlmOptions options;

    AiFeature(int maxTokens, Double temperature) {
        this.options = new LlmOptions(maxTokens, temperature, null);
    }

    public LlmOptions options() {
        return options;
    }
}
