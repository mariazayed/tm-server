package trademind.tm_server.invest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import trademind.tm_server.invest.price.PriceProperties;
import trademind.tm_server.invest.reminder.ReminderProperties;

@Configuration
@EnableConfigurationProperties({PriceProperties.class, ReminderProperties.class})
public class InvestConfig {
}
