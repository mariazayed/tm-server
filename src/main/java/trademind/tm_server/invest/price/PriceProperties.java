package trademind.tm_server.invest.price;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param enabled     turns the daily refresh job on; the manual refresh endpoint always works
 * @param refreshCron Spring cron (UTC) for the daily job; defaults to 23:30 every day
 * @param baseUrl     Yahoo Finance chart API base URL
 */
@ConfigurationProperties(prefix = "invest.prices")
public record PriceProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("0 30 23 * * *") String refreshCron,
        @DefaultValue("https://query1.finance.yahoo.com") String baseUrl) {
}
