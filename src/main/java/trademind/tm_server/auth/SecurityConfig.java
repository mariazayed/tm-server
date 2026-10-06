package trademind.tm_server.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import trademind.tm_server.constants.AppConstants;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

/**
 * Locks down the investing-tracker routes only. The pre-existing trade-journal endpoints keep
 * their current open behaviour so the dashboard and tm-py-server importer are unaffected.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer tokens, no cookies
                .cors(Customizer.withDefaults()) // honours the existing @CrossOrigin settings
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AppConstants.URL_AUTH + "/login").permitAll()
                        .requestMatchers(AppConstants.URL_AUTH + "/me").authenticated()
                        .requestMatchers(AppConstants.URL_INVEST + "/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LoginRateLimiter loginRateLimiter() {
        return new LoginRateLimiter(5, Duration.ofMinutes(15));
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties) {
        return NimbusJwtDecoder.withSecretKey(signingKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * When no secret is configured a random per-boot key is used, so no token can ever validate
     * and the protected routes stay closed.
     */
    private static SecretKey signingKey(AuthProperties properties) {
        byte[] bytes;
        if (properties.configured()) {
            bytes = properties.sessionSecret().getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = new byte[AuthProperties.MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(bytes);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
