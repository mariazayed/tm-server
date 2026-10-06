package trademind.tm_server.invest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import trademind.tm_server.invest.price.PriceProperties;

@Configuration
@EnableConfigurationProperties(PriceProperties.class)
public class InvestConfig {
}
