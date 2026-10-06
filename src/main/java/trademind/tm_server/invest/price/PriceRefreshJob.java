package trademind.tm_server.invest.price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily refresh; only exists when {@code invest.prices.enabled=true}. */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "invest.prices", name = "enabled", havingValue = "true")
public class PriceRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshJob.class);

    private final PriceService service;

    public PriceRefreshJob(PriceService service) {
        this.service = service;
    }

    @Scheduled(cron = "${invest.prices.refresh-cron:0 30 23 * * *}", zone = "UTC")
    public void run() {
        PriceService.RefreshResult r = service.refresh();
        if (r.error() == null) {
            log.info("Price refresh ok: {} fetched, {} new, {} updated, latest {}",
                    r.fetched(), r.inserted(), r.updated(), r.latestDate());
        }
    }
}
