package trademind.tm_server.features;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import trademind.tm_server.constants.AppConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Answers 404 for the trading-only API while {@code features.trading.enabled} is false, so hiding
 * trading in the UI is not the only thing standing between a client and those routes. The routes
 * simply look absent; the controllers and the data behind them are untouched.
 *
 * <p>Only trading-only prefixes are listed. Shared areas (goals, decisions, planner,
 * expenses, static data) stay open.
 */
@Component
public class TradingGateFilter extends OncePerRequestFilter {

    static final List<String> TRADING_PREFIXES = List.of(
            AppConstants.URL_TRADE,
            AppConstants.URL_TRADE_STEP,
            AppConstants.URL_TRADE_INSIGHT,
            AppConstants.URL_DAY,
            AppConstants.URL_STRATEGIES,
            AppConstants.URL_ASSET,
            AppConstants.URL_CAPITAL,
            AppConstants.URL_STRICT_RULES);

    private final FeatureFlags flags;

    public TradingGateFilter(FeatureFlags flags) {
        this.flags = flags;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return flags.trading() || !isTradingPath(request.getRequestURI());
    }

    /** Matches the prefix itself or anything below it, so {@code /trade} does not catch {@code /tradeStep}. */
    static boolean isTradingPath(String path) {
        for (String prefix : TRADING_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write("{\"error\":\"Not found\"}".getBytes(StandardCharsets.UTF_8));
    }
}
