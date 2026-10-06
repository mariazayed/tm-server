package trademind.tm_server.invest.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily reminder check; only exists when {@code invest.reminders.enabled=true}. */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "invest.reminders", name = "enabled", havingValue = "true")
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final ReminderService service;

    public ReminderJob(ReminderService service) {
        this.service = service;
    }

    @Scheduled(cron = "${invest.reminders.cron:0 0 9 * * *}", zone = "UTC")
    public void run() {
        ReminderService.RunResult r = service.runDaily();
        if (r.sent()) {
            log.info("Sent {} reminder", r.kind());
        } else if (r.error() != null) {
            log.warn("Reminder not sent: {}", r.error());
        }
    }
}
