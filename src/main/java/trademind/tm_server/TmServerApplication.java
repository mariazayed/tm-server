package trademind.tm_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TmServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmServerApplication.class, args);
    }

}
