package trademind.tm_server.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = AuthSecurityTest.TestApp.class)
@Import({SecurityConfig.class, TokenService.class, AuthController.class, InvestController.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.auth.session-secret=0123456789abcdef0123456789abcdef-test")
class AuthSecurityTest {

    static final String PASSWORD = "correct horse battery";

    @DynamicPropertySource
    static void passwordHash(DynamicPropertyRegistry registry) {
        registry.add("app.auth.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    MockMvc mvc;

    @Test
    void protectedRoutesRejectMissingAndBadTokens() throws Exception {
        mvc.perform(get("/api/v1/invest/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/invest/ping").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validLoginYieldsTokenThatOpensProtectedRoutes() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/v1/invest/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void existingEndpointsStayOpen() throws Exception {
        // No handler is mounted for this path in the slice, so the app's catch-all handler
        // answers; what matters is that the security chain did not reject it as 401/403.
        mvc.perform(get("/api/v1/goal")).andExpect(result -> {
            int code = result.getResponse().getStatus();
            assertNotEquals(401, code);
            assertNotEquals(403, code);
        });
    }
}
