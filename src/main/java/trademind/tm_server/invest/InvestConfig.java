package trademind.tm_server.invest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import trademind.tm_server.invest.ai.AiProperties;
import trademind.tm_server.invest.backup.BackupProperties;
import trademind.tm_server.invest.price.PriceProperties;
import trademind.tm_server.invest.reminder.ReminderProperties;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({PriceProperties.class, ReminderProperties.class, BackupProperties.class,
        AiProperties.class})
public class InvestConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
