package trademind.tm_server.invest.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nightly backup; only exists when {@code invest.backup.enabled=true}. */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "invest.backup", name = "enabled", havingValue = "true")
public class BackupJob {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);

    private final BackupService service;

    public BackupJob(BackupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${invest.backup.cron:0 30 2 * * *}", zone = "UTC")
    public void run() {
        try {
            service.backup();
        } catch (RuntimeException e) {
            // Never let a failed backup kill the scheduler thread; the next night tries again.
            log.error("Nightly backup failed: {}", e.getMessage(), e);
        }
    }
}
