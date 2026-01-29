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
    public static final String DB_DAYS = "days";
    public static final String DB_MEMOIRS = "memoirs";
    public static final String DB_CAPITALS = "capitals";
    public static final String DB_REMINDER_NOTES = "reminder_notes";

    // URL Endpoints
    private static final String API_VERSION = "/api/v1";
    public static final String URL_ASSET = API_VERSION + "/asset";
    public static final String URL_TRADE = API_VERSION + "/trade";
    public static final String URL_TRADE_PLAN = API_VERSION + "/tradePlan";
    public static final String URL_TRADE_EXECUTION = API_VERSION + "/tradeExecution";
    public static final String URL_TRADE_MONITORING = API_VERSION + "/tradeMonitoring";
    public static final String URL_TRADE_TERMINATION = API_VERSION + "/tradeTermination";
    public static final String URL_TRADE_EVALUATION = API_VERSION + "/tradeEvaluation";
    public static final String URL_DAY = API_VERSION + "/day";
    public static final String URL_MEMOIR = API_VERSION + "/memoir";
    public static final String URL_CAPITAL = API_VERSION + "/capital";
    public static final String URL_REMINDER_NOTES = API_VERSION + "/reminderNotes";
    public static final String URL_STATIC_DATA = API_VERSION + "/staticData";

    // Sub-URL Endpoints
    public static final String SUB_URL_SEARCH = "/search";
    public static final String SUB_URL_UNDO_DELETE = "/undoDelete/{id}";
    public static final String SUB_URL_ID_PARAM = "/{id}";
}
