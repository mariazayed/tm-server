package trademind.tm_server.features;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingGateFilterTest {

    private static final String[] TRADING = {
            "/api/v1/trade", "/api/v1/trade/search", "/api/v1/trade/5/review", "/api/v1/tradeStep",
            "/api/v1/tradeInsight/3", "/api/v1/day", "/api/v1/day/2026-01-02", "/api/v1/strategies",
            "/api/v1/asset", "/api/v1/asset/1", "/api/v1/capital", "/api/v1/capital/search",
            "/api/v1/strictRules", "/api/v1/strictRules/2"};

    private static final String[] OPEN = {
            "/api/v1/invest/contributions", "/api/v1/invest/ai/chat", "/api/v1/features", "/api/v1/goal",
            "/api/v1/decision", "/api/v1/planner", "/api/v1/expense", "/api/v1/expenseReason",
            "/api/v1/questionAnswers", "/api/v1/reminderNotes", "/api/v1/staticData",
            "/api/v1/tradePlan-not-a-route", "/api/v1/dayOff"};

    private MockHttpServletResponse call(boolean tradingOn, String path)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        new TradingGateFilter(new FeatureFlags(tradingOn))
                .doFilter(new MockHttpServletRequest("GET", path), response, chain);
        // The chain only has a request recorded if the filter let it through.
        if (chain.getRequest() != null) response.setForwardedUrl("passed");
        return response;
    }

    @Test
    void tradingRoutesAre404WhenOff() throws Exception {
        for (String path : TRADING) {
            MockHttpServletResponse r = call(false, path);
            assertEquals(404, r.getStatus(), path);
            assertNull(r.getForwardedUrl(), path);
            assertTrue(r.getContentAsString().contains("Not found"), path);
        }
    }

    @Test
    void everyOtherRoutePassesThroughWhenOff() throws Exception {
        for (String path : OPEN) {
            MockHttpServletResponse r = call(false, path);
            assertEquals("passed", r.getForwardedUrl(), path);
            assertEquals(200, r.getStatus(), path);
        }
    }

    @Test
    void tradingRoutesPassThroughWhenOn() throws Exception {
        for (String path : TRADING) {
            MockHttpServletResponse r = call(true, path);
            assertEquals("passed", r.getForwardedUrl(), path);
            assertEquals(200, r.getStatus(), path);
        }
    }

    @Test
    void prefixMatchRespectsPathBoundaries() {
        assertTrue(TradingGateFilter.isTradingPath("/api/v1/trade"));
        assertTrue(TradingGateFilter.isTradingPath("/api/v1/trade/1"));
        assertFalse(TradingGateFilter.isTradingPath("/api/v1/tradeX"));
        assertNotNull(TradingGateFilter.TRADING_PREFIXES);
    }
}
