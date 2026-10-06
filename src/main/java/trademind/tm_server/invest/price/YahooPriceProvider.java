package trademind.tm_server.invest.price;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily closes from Yahoo Finance's chart endpoint (unofficial, key-less). Raw (not dividend
 * adjusted) closes are used, so they are comparable with the price you actually paid.
 */
@Component
public class YahooPriceProvider implements PriceProvider {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final RestClient client;

    @Autowired
    public YahooPriceProvider(PriceProperties properties) {
        // Not injected: spring-boot-restclient (the RestClient.Builder bean) is test-scope only.
        this(properties, RestClient.builder());
    }

    YahooPriceProvider(PriceProperties properties, RestClient.Builder builder) {
        this.client = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (invest-tracker)")
                .build();
    }

    @Override
    public List<DailyClose> fetchDailyCloses(String ticker, LocalDate from, LocalDate to) {
        long period1 = from.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long period2 = to.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        String body;
        try {
            body = client.get()
                    .uri("/v8/finance/chart/{t}?period1={p1}&period2={p2}&interval=1d&events=history",
                            ticker, period1, period2)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new PriceProviderException("Price request failed: " + e.getMessage(), e);
        }
        return parse(body, from, to);
    }

    static List<DailyClose> parse(String json, LocalDate from, LocalDate to) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (RuntimeException e) {
            throw new PriceProviderException("Price response was not valid JSON", e);
        }
        JsonNode error = root.path("chart").path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new PriceProviderException("Price provider error: " + error.path("description").asString("unknown"));
        }
        JsonNode result = root.path("chart").path("result").path(0);
        JsonNode timestamps = result.path("timestamp");
        JsonNode closes = result.path("indicators").path("quote").path(0).path("close");
        if (result.isMissingNode() || result.isNull()) {
            throw new PriceProviderException("Price response had no data");
        }
        if (!timestamps.isArray()) {
            return List.of(); // a range with no trading days
        }
        if (!closes.isArray() || closes.size() != timestamps.size()) {
            throw new PriceProviderException("Price response was malformed");
        }
        int offsetSeconds = result.path("meta").path("gmtoffset").asInt(0);

        List<DailyClose> out = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            JsonNode close = closes.get(i);
            if (close == null || close.isNull() || !close.isNumber()) continue; // halted/missing day
            BigDecimal value = new BigDecimal(close.asString()).setScale(4, java.math.RoundingMode.HALF_UP);
            if (value.signum() <= 0) continue;
            LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong() + offsetSeconds)
                    .atOffset(ZoneOffset.UTC).toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to)) {
                out.add(new DailyClose(date, value));
            }
        }
        out.sort(java.util.Comparator.comparing(DailyClose::date));
        return out;
    }
}
