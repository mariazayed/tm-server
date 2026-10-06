package trademind.tm_server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.exceptions.UnauthorizedException;

import java.time.Instant;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_AUTH)
public class AuthController {

    public record LoginRequest(@NotBlank @Size(max = 200) String password) {
    }

    public record LoginResponse(String token, Instant expiresAt) {
    }

    public record MeResponse(String user) {
    }

    private final AuthProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthProperties properties, PasswordEncoder passwordEncoder,
                          TokenService tokenService, LoginRateLimiter rateLimiter) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        if (!properties.configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Login is not configured on the server");
        }
        String clientKey = http.getRemoteAddr();
        LoginRateLimiter.Result limit = rateLimiter.check(clientKey);
        if (!limit.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(limit.retryAfterSeconds()))
                    .build();
        }
        if (!passwordEncoder.matches(request.password(), properties.passwordHash())) {
            throw new UnauthorizedException("Incorrect password");
        }
        rateLimiter.reset(clientKey);
        TokenService.IssuedToken issued = tokenService.issue();
        return ResponseEntity.ok(new LoginResponse(issued.token(), issued.expiresAt()));
    }

    /** Lets the client verify a stored token is still valid. */
    @GetMapping("/me")
    public MeResponse me() {
        return new MeResponse(TokenService.OWNER_SUBJECT);
    }
}
