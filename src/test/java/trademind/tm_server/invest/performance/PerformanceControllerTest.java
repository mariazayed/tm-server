package trademind.tm_server.invest.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.auth.SecurityConfig;
import trademind.tm_server.auth.TokenService;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.price.PriceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = PerformanceControllerTest.TestApp.class)
@Import({SecurityConfig.class, TokenService.class, PerformanceController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.auth.session-secret=0123456789abcdef0123456789abcdef-test")
class PerformanceControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean PerformanceService performance;
    @MockitoBean PriceService prices;

    @Test
    void bothRoutesRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/invest/performance")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/invest/prices/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPerformanceJson() throws Exception {
        when(performance.performance("1Y")).thenReturn(new PerformanceResponse("VT", "USD", 60100, 100,
                new BigDecimal("5.5"), new BigDecimal("109.2727"), new BigDecimal("130.0000"),
                LocalDate.parse("2026-09-10"), true, false, 71500, 11400, new BigDecimal("18.97"), "1Y",
                List.of(new PerformanceResponse.SeriesPoint(LocalDate.parse("2026-09-10"), 60100, 71500))));

        mvc.perform(get("/api/v1/invest/performance?range=1Y").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gainPercent").value(18.97))
                .andExpect(jsonPath("$.series[0].valueCents").value(71500))
                .andExpect(jsonPath("$.priceAsOf").value("2026-09-10"));
    }

    @Test
    void badRangeIs400() throws Exception {
        when(performance.performance("5Y")).thenThrow(new BadRequestException("range must be one of 3M, 6M, 1Y, ALL"));
        mvc.perform(get("/api/v1/invest/performance?range=5Y").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void refreshReportsProviderFailureInTheBodyNotAsAnHttpError() throws Exception {
        when(prices.refresh()).thenReturn(new PriceService.RefreshResult(0, 0, 0, LocalDate.parse("2026-10-09"), "HTTP 429"));
        mvc.perform(post("/api/v1/invest/prices/refresh").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("HTTP 429"))
                .andExpect(jsonPath("$.latestDate").value("2026-10-09"));
    }
}
