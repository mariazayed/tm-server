package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import trademind.tm_server.auth.SecurityConfig;
import trademind.tm_server.auth.TokenService;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = AiControllerTest.TestApp.class)
@Import({SecurityConfig.class, TokenService.class, AiController.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
@TestPropertySource(properties = {
        "app.auth.session-secret=0123456789abcdef0123456789abcdef-test",
        "ai.provider.api-key=sk-ant-THIS-MUST-NEVER-LEAK",
        "ai.provider.model=claude-test-model"
})
class AiControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    private static final String SECRET = "sk-ant-THIS-MUST-NEVER-LEAK";

    @Autowired MockMvc mvc;
    @MockitoBean UserContextService contextService;

    private static UserContext context() {
        return new UserContext(Instant.parse("2026-10-06T12:00:00Z"),
                new UserContext.Plan(30000, "USD", 1, "VT", LocalDate.parse("2026-08-01")), List.of("Wait 72 hours."),
                new UserContext.Portfolio(30000, 33214, 3214, new BigDecimal("10.71"), new BigDecimal("2.55489022"),
                        new BigDecimal("117.4251"), LocalDate.parse("2026-10-05"), true, false),
                new UserContext.Consistency("DONE", 1, 1, 2, LocalDate.parse("2026-10-01")), List.of(), List.of(), List.of(),
                new UserContext.Cards(1, 0, 0), false);
    }

    @Test
    void bothRoutesRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/invest/ai/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/invest/ai/context")).andExpect(status().isUnauthorized());
    }

    @Test
    void statusSaysConfiguredAndNamesTheModelButNeverTheKey() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.model").value("claude-test-model"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
        assertFalse(result.getResponse().getContentAsString().toLowerCase().contains("api-key\":\"sk"));
    }

    @Test
    void statusExplainsWhatIsMissing() {
        assertEquals(new AiController.Status(false, null, "Set ai.provider.api-key and ai.provider.model on the server"),
                AiController.status("", ""));
        assertEquals(new AiController.Status(false, null, "Set ai.provider.api-key and ai.provider.model on the server"),
                AiController.status(null, "   "));
        assertEquals(new AiController.Status(false, "m", "Set ai.provider.api-key on the server"),
                AiController.status(" ", "m"));
        assertEquals(new AiController.Status(false, null, "Set ai.provider.model on the server"),
                AiController.status("k", ""));
        assertEquals(new AiController.Status(true, "m", null), AiController.status("k", " m "));
    }

    @Test
    void contextPreviewShowsExactlyTheTextAndItsSize() throws Exception {
        when(contextService.build(any())).thenReturn(context());
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/context").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includeJournal").value(true))
                .andExpect(jsonPath("$.includeNotes").value(true))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.maxCharacters").value(UserContextRenderer.MAX_CHARS))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Wait 72 hours.")))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
    }

    @Test
    void thePrivacyTogglesReachTheService() throws Exception {
        when(contextService.build(any())).thenReturn(context());
        mvc.perform(get("/api/v1/invest/ai/context").param("includeJournal", "false").param("includeNotes", "false").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includeJournal").value(false))
                .andExpect(jsonPath("$.includeNotes").value(false));
        verify(contextService).build(eq(new ContextOptions(false, false)));
    }
}
