package trademind.tm_server.constants;

public final class AppConstants {
    // Prevent instantiation
    private AppConstants() {
    }

    // Database tables names
    public static final String DB_ASSETS = "assets";
    public static final String DB_TRADES = "trades";
    public static final String DB_TRADE_PLANS = "trade_plans";
    public static final String DB_TRADE_EXECUTIONS = "trade_executions";
    public static final String DB_TRADE_MONITORING = "trade_monitoring";
    public static final String DB_TRADE_TERMINATIONS = "trade_terminations";
    public static final String DB_TRADE_EVALUATIONS = "trade_evaluations";
    public static final String DB_TRADE_STEPS = "trade_steps";
    public static final String DB_TRADE_INSIGHTS = "trade_insights";
    public static final String DB_DAYS = "days";
    public static final String DB_CAPITALS = "capitals";
    public static final String DB_REMINDER_NOTES = "reminder_notes";
    public static final String DB_STRATEGIES = "strategies";
    public static final String DB_STRICT_RULES = "strict_rules";
    public static final String DB_GOALS = "goals";

    // URL Endpoints
    private static final String API_VERSION = "/api/v1";
    public static final String URL_ASSET = API_VERSION + "/asset";
    public static final String URL_TRADE = API_VERSION + "/trade";
    public static final String URL_TRADE_STEP = API_VERSION + "/tradeStep";
    public static final String URL_TRADE_INSIGHT = API_VERSION + "/tradeInsight";
    public static final String URL_TRADE_PLAN = API_VERSION + "/tradePlan";
    public static final String URL_TRADE_EXECUTION = API_VERSION + "/tradeExecution";
    public static final String URL_TRADE_MONITORING = API_VERSION + "/tradeMonitoring";
    public static final String URL_TRADE_TERMINATION = API_VERSION + "/tradeTermination";
    public static final String URL_TRADE_EVALUATION = API_VERSION + "/tradeEvaluation";
    public static final String URL_DAY = API_VERSION + "/day";
    public static final String URL_CAPITAL = API_VERSION + "/capital";
    public static final String URL_REMINDER_NOTES = API_VERSION + "/reminderNotes";
    public static final String URL_STRICT_RULES = API_VERSION + "/strictRules";
    public static final String URL_GOAL = API_VERSION + "/goal";
    public static final String URL_STRATEGIES = API_VERSION + "/strategies";
    public static final String URL_STATIC_DATA = API_VERSION + "/staticData";

    // Sub-URL Endpoints
    public static final String SUB_URL_SEARCH = "/search";
    public static final String SUB_URL_UNDO_DELETE = "/undoDelete/{id}";
    public static final String SUB_URL_ID_PARAM = "/{id}";
    public static final String SUB_URL_BY_TRADE_PARAM = "/byTrade/{tradeId}";
    public static final String SUB_URL_REVIEW = "/{id}/review";
    public static final String SUB_URL_SUGGEST = "/{id}/suggest";

    // Trade enrichment thresholds (Phase 3) -- single source of truth for
    // TradeEnrichmentService, so behavior is documented and centralized
    // rather than scattered as magic numbers.
    public static final int SCALPING_MAX_DURATION_MINUTES = 15;
    public static final int INTRADAY_MAX_DURATION_MINUTES = 24 * 60;
    public static final int LONDON_SESSION_START_HOUR_UTC = 8;
    public static final int NEW_YORK_SESSION_START_HOUR_UTC = 13;
}
