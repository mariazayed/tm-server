package trademind.tm_server.invest.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param enabled turns the nightly job on
 * @param dir     where backup files are written; created if missing
 * @param keep    how many of the newest backups to keep (older ones are deleted)
 * @param cron    Spring cron, UTC; default 02:30 every night
 */
@ConfigurationProperties(prefix = "invest.backup")
public record BackupProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("backups") String dir,
        @DefaultValue("14") int keep,
        @DefaultValue("0 30 2 * * *") String cron) {

    public BackupProperties {
        if (keep < 1 || keep > 3650) {
            throw new IllegalArgumentException("invest.backup.keep must be between 1 and 3650");
        }
    }
}
