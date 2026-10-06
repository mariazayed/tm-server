package trademind.tm_server.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Issues signed HS256 session tokens for the single owner. */
@Service
public class TokenService {

    public static final String OWNER_SUBJECT = "owner";

    public record IssuedToken(String token, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue() {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofDays(properties.sessionDays()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(OWNER_SUBJECT)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
