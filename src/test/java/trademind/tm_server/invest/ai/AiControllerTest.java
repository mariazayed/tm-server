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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = AiControllerTest.TestApp.class)
@Import({AiController.class, AiService.class, AiControllerTest.Wiring.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
@TestPropertySource(properties = {
        "ai.enabled=true",
        "ai.provider.api-key=sk-ant-THIS-MUST-NEVER-LEAK",
        "ai.provider.model=claude-test-model"
})
class AiControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    /** Real AiService with a scripted model and in-memory counts, so the HTTP mapping is tested end to end. */
    // Deliberately not @Configuration/@TestConfiguration: InvestEndToEndTest component-scans this package.
    @org.springframework.boot.context.properties.EnableConfigurationProperties(AiProperties.class)
    static class Wiring {
        @org.springframework.context.annotation.Bean
        java.time.Clock clock() {
            return java.time.Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), java.time.ZoneOffset.UTC);
        }

        @org.springframework.context.annotation.Bean
        AiUsageStore usageStore() {
            return new InMemoryAiUsageStore();
        }
    }

    private static final String SECRET = "sk-ant-THIS-MUST-NEVER-LEAK";

    @Autowired MockMvc mvc;
    @MockitoBean UserContextService contextService;
    @MockitoBean trademind.tm_server.ai.LlmClient llm;
    @Autowired AiService ai;

    private static UserContext context() {
        return new UserContext(Instant.parse("2026-10-06T12:00:00Z"),
                new UserContext.Plan(30000, "USD", 1, "VT", LocalDate.parse("2026-08-01")), List.of("Wait 72 hours."),
                new UserContext.Portfolio(30000, 33214, 3214, new BigDecimal("10.71"), new BigDecimal("2.55489022"),
                        new BigDecimal("117.4251"), LocalDate.parse("2026-10-05"), true, false),
                new UserContext.Consistency("DONE", 1, 1, 2, LocalDate.parse("2026-10-01")), List.of(), List.of(), List.of(),
                new UserContext.Cards(1, 0, 0), false);
    }

    @Test
    void statusSaysConfiguredAndNamesTheModelButNeverTheKey() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.model").value("claude-test-model"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
        assertFalse(result.getResponse().getContentAsString().toLowerCase().contains("api-key\":\"sk"));
    }

    @Test
    void usageReportsTodaysCallsAgainstTheCaps() throws Exception {
        when(llm.completeWithUsage(any(), any(), any()))
                .thenReturn(new trademind.tm_server.ai.LlmResult("ok", 120, 30, "end_turn"));
        ai.call(AiFeature.CHAT, "s", "u");
        mvc.perform(get("/api/v1/invest/ai/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-06"))
                .andExpect(jsonPath("$.callsToday").value(1))
                .andExpect(jsonPath("$.dailyLimit").value(60))
                .andExpect(jsonPath("$.perMinuteLimit").value(6))
                .andExpect(jsonPath("$.inputTokens").value(120))
                .andExpect(jsonPath("$.outputTokens").value(30))
                .andExpect(jsonPath("$.callsByFeature.CHAT").value(1));
    }

    @Test
    void statusExplainsWhatIsMissing() {
        assertEquals(new AiController.Status(false, "m", "AI is switched off. Set ai.enabled=true on the server"),
                AiController.status(false, "k", " m "));
        assertEquals(new AiController.Status(false, null, "AI is switched off. Set ai.enabled=true on the server"),
                AiController.status(false, "", ""));
        assertEquals(new AiController.Status(true, "m", null), AiController.status(true, "k", "m"));

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
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/context"))
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
        mvc.perform(get("/api/v1/invest/ai/context").param("includeJournal", "false").param("includeNotes", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includeJournal").value(false))
                .andExpect(jsonPath("$.includeNotes").value(false));
        verify(contextService).build(eq(new ContextOptions(false, false)));
    }
}
