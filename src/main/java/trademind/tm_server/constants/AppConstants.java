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
    public static final String DB_DECISIONS = "decisions";
    public static final String DB_QUESTION_CATEGORIES = "question_categories";
    public static final String DB_QUESTION_ANSWERS = "question_answers";
    public static final String DB_EXPENSE_REASONS = "expense_reasons";
    public static final String DB_EXPENSES = "expenses";
    public static final String DB_FOCUS_AREAS = "focus_areas";
    public static final String DB_PLANNER_SETTINGS = "planner_settings";
    public static final String DB_PLANNER_WEEKS = "planner_weeks";
    public static final String DB_MONTHLY_OBJECTIVES = "monthly_objectives";
    public static final String DB_WEEKLY_TASKS = "weekly_tasks";
    public static final String DB_PLANNER_WEEK_EXTRAS = "planner_week_extras";
    public static final String DB_INVEST_SETTINGS = "invest_settings";
    public static final String DB_INVEST_CONTRIBUTIONS = "invest_contributions";
    public static final String DB_INVEST_HOLDINGS = "invest_holdings";
    public static final String DB_INVEST_PRICES = "invest_prices";
    public static final String DB_INVEST_TAGS = "invest_tags";
    public static final String DB_INVEST_JOURNAL_ENTRIES = "invest_journal_entries";
    public static final String DB_INVEST_JOURNAL_ENTRY_TAGS = "invest_journal_entry_tags";
    public static final String DB_INVEST_RULES = "invest_rules";
    public static final String DB_INVEST_SOURCES = "invest_sources";
    public static final String DB_INVEST_LEARNING_NOTES = "invest_learning_notes";
    public static final String DB_INVEST_LEARNING_NOTE_TAGS = "invest_learning_note_tags";
    public static final String DB_INVEST_FLASHCARDS = "invest_flashcards";
    public static final String DB_INVEST_REMINDER_LOG = "invest_reminder_log";
    public static final String DB_INVEST_EMBEDDINGS = "invest_embeddings";
    public static final String DB_INVEST_SETTINGS_CHANGES = "invest_settings_changes";

    // URL Endpoints
    private static final String API_VERSION = "/api/v1";
    public static final String URL_INVEST = API_VERSION + "/invest";
    public static final String URL_FEATURES = API_VERSION + "/features";
    public static final String URL_ARCHIVE = API_VERSION + "/archive";
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
    public static final String URL_REMINDER_NOTE_CATEGORIES = API_VERSION + "/reminderNoteCategories";
    public static final String URL_STRICT_RULES = API_VERSION + "/strictRules";
    public static final String URL_GOAL = API_VERSION + "/goal";
    public static final String URL_DECISION = API_VERSION + "/decision";
    public static final String URL_QUESTION_CATEGORIES = API_VERSION + "/questionCategories";
    public static final String URL_QUESTION_ANSWERS = API_VERSION + "/questionAnswers";
    public static final String URL_EXPENSE_REASON = API_VERSION + "/expenseReason";
    public static final String URL_EXPENSE = API_VERSION + "/expense";
    public static final String URL_PLANNER = API_VERSION + "/planner";
    public static final String URL_STRATEGIES = API_VERSION + "/strategies";
    public static final String URL_STATIC_DATA = API_VERSION + "/staticData";

    // Sub-URL Endpoints
    public static final String SUB_URL_SEARCH = "/search";
    public static final String SUB_URL_UNDO_DELETE = "/undoDelete/{id}";
    public static final String SUB_URL_ID_PARAM = "/{id}";
    public static final String SUB_URL_BY_TRADE_PARAM = "/byTrade/{tradeId}";
    public static final String SUB_URL_REVIEW = "/{id}/review";
    public static final String SUB_URL_SUGGEST = "/{id}/suggest";
    public static final String SUB_URL_RECOMPUTE_ENRICHMENT = "/recomputeEnrichment";

    // Trade enrichment thresholds (Phase 3) -- single source of truth for
    // TradeEnrichmentService, so behavior is documented and centralized
    // rather than scattered as magic numbers.
    public static final int SCALPING_MAX_DURATION_MINUTES = 15;
    public static final int INTRADAY_MAX_DURATION_MINUTES = 24 * 60;
    public static final int LONDON_SESSION_START_HOUR_UTC = 8;
    public static final int NEW_YORK_SESSION_START_HOUR_UTC = 13;
}
