package trademind.tm_server.constants;

public final class AppConstants {
    // Prevent instantiation
    private AppConstants() {
    }

    // Database tables names
//    public static final String DB_STRATEGY = "strategies";
//    public static final String DB_PRICE_TRIGGERS = "price_triggers";
//    public static final String DB_SECTORS = "sectors";
    public static final String DB_ASSETS = "assets";
    //    public static final String DB_TRADES_DETAILS = "trades_details";
//    public static final String DB_TRADES = "trades";
    public static final String DB_TRADE_PLANS = "trade_plans";
    //    public static final String DB_STOP_LOSS_STRATEGY = "stop_loss_strategy";
//    public static final String DB_TAKE_PROFIT_STRATEGY = "take_profit_strategy";
//
    // URL Endpoints
    private static final String API_VERSION = "/api/v1";
    //    public static final String URL_STRATEGY = API_VERSION + "/strategy";
//    public static final String URL_PRICE_TRIGGER = API_VERSION + "/priceTrigger";
//    public static final String URL_SECTOR = API_VERSION + "/sector";
//    public static final String URL_TRADE_DETAILS = API_VERSION + "/tradeDetails";
//    public static final String URL_TRADE = API_VERSION + "/trade";
    public static final String URL_ASSET = API_VERSION + "/asset";
    //    public static final String URL_STOP_LOSS_STRATEGY = API_VERSION + "/stopLossStrategy";
//    public static final String URL_TAKE_PROFIT_STRATEGY = API_VERSION + "/takeProfitStrategy";
//
//
    // Sub-URL Endpoints
    public static final String SUB_URL_SEARCH = "/search";
    public static final String SUB_URL_UNDO_DELETE = "/undoDelete/{id}";
    public static final String SUB_URL_ID_PARAM = "/{id}";
}
