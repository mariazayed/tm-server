package trademind.tm_server.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    // Access from: http://localhost:8080/swagger-ui/index.html

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TraderBeast API")
                        .version("1.0.0")
                        .description("REST API for managing trading assets, trades, sectors, and related operations. " +
                                "This API provides comprehensive endpoints for tracking and analyzing trading activities.")
                        .contact(new Contact()
                                .name("Maria Zayed")
                                .email("mariawzayed@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                )
                .addServersItem(new Server()
                        .description("Local development server"));
//                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
//                .components(new Components()
//                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
//                                new SecurityScheme()
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer")
//                                        .bearerFormat("JWT")
//                                        .description("JWT authentication token")
//                                        .name(SECURITY_SCHEME_NAME)
//                        )
//                );
    }
}