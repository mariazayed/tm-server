package trademind.tm_server.invest.reminder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param enabled turns the daily reminder job on (each user-level switch lives in the settings)
 * @param cron    Spring cron, UTC; default 09:00 every day
 * @param from    sender address of the reminder emails
 */
@ConfigurationProperties(prefix = "invest.reminders")
public record ReminderProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("0 0 9 * * *") String cron,
        String from) {
}
