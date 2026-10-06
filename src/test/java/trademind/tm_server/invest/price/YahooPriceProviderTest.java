package trademind.tm_server.invest.price;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Parsing is checked against payloads shaped like Yahoo's chart API. The live endpoint was not
 * reachable from the build sandbox, so the first real refresh is the true end-to-end check.
 */
class YahooPriceProviderTest {

    // 2026-10-01 14:30 UTC (Thu), 2026-10-02 14:30 UTC (Fri, null close), 2026-10-05 14:30 UTC (Mon).
    private static final String PAYLOAD = """
            {"chart":{"result":[{"meta":{"symbol":"VT","gmtoffset":-14400},
             "timestamp":[1790865000,1790951400,1791210600],
             "indicators":{"quote":[{"close":[117.43000030517578,null,118.9]}]}}],"error":null}}""";

    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-06");

    @Test
    void parsesClosesSkipsNullsAndUsesTheExchangeOffsetForDates() {
        List<PriceProvider.DailyClose> closes = YahooPriceProvider.parse(PAYLOAD, FROM, TO);
        assertEquals(2, closes.size());
        assertEquals(LocalDate.parse("2026-10-01"), closes.get(0).date());
        assertEquals(new BigDecimal("117.4300"), closes.get(0).close());
        assertEquals(LocalDate.parse("2026-10-05"), closes.get(1).date());
    }

    @Test
    void dropsRowsOutsideTheRequestedRange() {
        assertEquals(1, YahooPriceProvider.parse(PAYLOAD, LocalDate.parse("2026-10-02"), TO).size());
    }

    @Test
    void emptyRangeIsNotAnError() {
        String noDays = "{\"chart\":{\"result\":[{\"meta\":{},\"indicators\":{\"quote\":[{}]}}],\"error\":null}}";
        assertTrue(YahooPriceProvider.parse(noDays, FROM, TO).isEmpty());
    }

    @Test
    void providerErrorsAndGarbageBecomePriceProviderExceptions() {
        String err = "{\"chart\":{\"result\":null,\"error\":{\"code\":\"Not Found\",\"description\":\"No data found\"}}}";
        assertThrows(PriceProvider.PriceProviderException.class, () -> YahooPriceProvider.parse(err, FROM, TO));
        assertThrows(PriceProvider.PriceProviderException.class, () -> YahooPriceProvider.parse("<html>", FROM, TO));
        String mismatch = "{\"chart\":{\"result\":[{\"timestamp\":[1,2],\"indicators\":{\"quote\":[{\"close\":[1]}]}}]}}";
        assertThrows(PriceProvider.PriceProviderException.class, () -> YahooPriceProvider.parse(mismatch, FROM, TO));
    }

    @Test
    void sendsTheExpectedRequestAndMapsHttpFailures() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        YahooPriceProvider provider = new YahooPriceProvider(new PriceProperties(false, "x", "https://prices.test"), builder);

        // Expectations are declared up front: the server is ordered and cannot be extended mid-run.
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://prices.test/v8/finance/chart/VT")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("interval", "1d"))
                .andExpect(header("User-Agent", org.hamcrest.Matchers.containsString("Mozilla")))
                .andRespond(withSuccess(PAYLOAD, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://prices.test/")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://prices.test/"))).andRespond(withServerError());

        assertEquals(2, provider.fetchDailyCloses("VT", FROM, TO).size());
        assertThrows(PriceProvider.PriceProviderException.class, () -> provider.fetchDailyCloses("VT", FROM, TO));
        assertThrows(PriceProvider.PriceProviderException.class, () -> provider.fetchDailyCloses("VT", FROM, TO));
        server.verify();
    }
}
