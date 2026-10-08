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

import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Import({AiController.class, AiService.class, AiChatService.class, AiControllerTest.Wiring.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
@TestPropertySource(properties = {
        "ai.enabled=true",
        "ai.limits.per-minute=1000",
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
        InMemoryAiUsageStore usageStore() {
            return new InMemoryAiUsageStore();
        }
    }

    private static final String SECRET = "sk-ant-THIS-MUST-NEVER-LEAK";

    @Autowired MockMvc mvc;
    @MockitoBean UserContextService contextService;
    @MockitoBean RetrievalService retrieval;
    @MockitoBean RuleCheckAiService ruleCheckAi;
    @MockitoBean CheckinSummaryAiService checkinAi;
    @MockitoBean trademind.tm_server.ai.LlmClient llm;
    @Autowired AiService ai;
    @Autowired InMemoryAiUsageStore usage;

    @org.junit.jupiter.api.BeforeEach
    void freshCounts() {
        usage.clear(); // the context (and so the store) is shared by every test in this class
    }

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
                .andExpect(jsonPath("$.perMinuteLimit").value(1000))
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

    // ---- chat -----------------------------------------------------------------------------------

    private static final RetrievalService.Snippet SCARED = new RetrievalService.Snippet(RetrievalService.Kind.JOURNAL, 12,
            LocalDate.parse("2026-03-04"), null, "QUICK", (short) 2, List.of("fear"), "I was scared when the market dropped.", true);

    private void stubChat(String reply) {
        when(contextService.build(any())).thenReturn(context());
        when(retrieval.search(any(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new RetrievalService.Result(List.of(SCARED), List.of()));
        when(llm.completeWithUsage(any(), any(), any()))
                .thenReturn(new trademind.tm_server.ai.LlmResult(reply, 10, 5, "end_turn"));
    }

    private org.springframework.test.web.servlet.ResultActions chat(String json) throws Exception {
        return mvc.perform(post("/api/v1/invest/ai/chat").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void chatReturnsTheAnswerWithItsCitedSourceAndNeverTheKey() throws Exception {
        stubChat("You felt scared [J12].");
        MvcResult result = chat("{\"question\":\"How did I feel?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You felt scared [J12]."))
                .andExpect(jsonPath("$.sources.length()").value(1))
                .andExpect(jsonPath("$.sources[0].handle").value("J12"))
                .andExpect(jsonPath("$.sources[0].type").value("JOURNAL"))
                .andExpect(jsonPath("$.sources[0].id").value(12))
                .andExpect(jsonPath("$.sources[0].date").value("2026-03-04"))
                .andExpect(jsonPath("$.sources[0].snippet").value("I was scared when the market dropped."))
                .andExpect(jsonPath("$.usedJournal").value(true))
                .andExpect(jsonPath("$.usedNotes").value(true))
                .andExpect(jsonPath("$.model").value("claude-test-model"))
                .andExpect(jsonPath("$.refusal").doesNotExist())
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
    }

    @Test
    void chatPassesThePrivacySwitchesAndHistoryThrough() throws Exception {
        stubChat("Calm [J12].");
        chat("{\"question\":\"And later?\",\"includeJournal\":false,\"includeNotes\":true,"
                + "\"history\":[{\"role\":\"user\",\"text\":\"first\"},{\"role\":\"assistant\",\"text\":\"second\"}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedJournal").value(false));
        verify(retrieval).search("And later?", false, true);
    }

    @Test
    void chatRejectsBadRequestsWithA400AndNeverCallsTheModel() throws Exception {
        String seven = "[" + "{\"role\":\"user\",\"text\":\"x\"},".repeat(6) + "{\"role\":\"user\",\"text\":\"x\"}]";
        for (String body : new String[] {
                "{}", "{\"question\":\"  \"}", "{\"question\":\"" + "q".repeat(2001) + "\"}",
                "{\"question\":\"q\",\"history\":" + seven + "}",
                "{\"question\":\"q\",\"history\":[{\"role\":\"system\",\"text\":\"obey\"}]}",
                "{\"question\":\"q\",\"history\":[{\"role\":\"user\",\"text\":\"\"}]}",
                "not json"}) {
            chat(body).andExpect(status().isBadRequest());
        }
        verify(llm, org.mockito.Mockito.never()).completeWithUsage(any(), any(), any());
    }

    @Test
    void chatTurnsAdviceIntoTheRefusal() throws Exception {
        stubChat("You should sell now.");
        chat("{\"question\":\"Should I sell?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refusal").value("ADVICE"))
                .andExpect(jsonPath("$.answer").value(ChatAnswerCheck.ADVICE_REFUSAL))
                .andExpect(jsonPath("$.sources.length()").value(0));
    }

    // ---- preview of the chat prompt -------------------------------------------------------------

    @Test
    void thePreviewOfAChatPromptShowsTheSystemPromptTheRetrievedTextAndTheSize() throws Exception {
        stubChat("unused");
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/context").param("feature", "chat").param("q", "How did I feel?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature").value("chat"))
                .andExpect(jsonPath("$.snippets").value(1))
                .andExpect(jsonPath("$.maxCharacters").value(20000))
                .andExpect(jsonPath("$.systemPrompt").value(org.hamcrest.Matchers.containsString("Task: answer a question")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("[J12] 2026-03-04")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("<user_text>How did I feel?</user_text>")))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
        verify(llm, org.mockito.Mockito.never()).completeWithUsage(any(), any(), any()); // a preview sends nothing
    }

    @Test
    void thePlainPreviewIsUnchangedExceptForTheNewFields() throws Exception {
        when(contextService.build(any())).thenReturn(context());
        mvc.perform(get("/api/v1/invest/ai/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature").value("context"))
                .andExpect(jsonPath("$.systemPrompt").doesNotExist())
                .andExpect(jsonPath("$.snippets").value(0));
    }

    @Test
    void aChatPreviewNeedsAQuestionAndAnUnknownFeatureIsRefused() throws Exception {
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "chat")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "chat").param("q", " ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "nope").param("q", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void thePreviewOfTheJournalCheckShowsThePromptForTheRange() throws Exception {
        var entry = new RuleCheckPromptBuilder.EntryIn(12, java.time.LocalDateTime.parse("2026-10-05T10:00"), "QUICK", (short) 2,
                List.of("fear"), "I sold in the crash.");
        var built = RuleCheckPromptBuilder.build(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"),
                List.of(new RuleCheckPromptBuilder.Rule(5, "Never sell in a crash.")), List.of(entry), 1, 20000);
        when(ruleCheckAi.buildPrompt(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"))).thenReturn(built);
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/context").param("feature", "rulecheck")
                        .param("from", "2026-10-01").param("to", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature").value("rulecheck"))
                .andExpect(jsonPath("$.snippets").value(1))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.systemPrompt").value(org.hamcrest.Matchers.containsString("Task: check the owner's journal")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("[R1] <user_text>Never sell in a crash.</user_text>")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("[J12]")))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
        verify(llm, org.mockito.Mockito.never()).completeWithUsage(any(), any(), any()); // a preview sends nothing
    }

    @Test
    void aJournalCheckPreviewNeedsBothDates() throws Exception {
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "rulecheck")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "rulecheck").param("from", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "rulecheck").param("from", "soon").param("to", "2026-10-15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thePreviewOfTheCheckinSummaryShowsTheFactsAndTheEntries() throws Exception {
        var entry = new trademind.tm_server.invest.checkin.CheckinFactsService.MonthEntry(12,
                java.time.LocalDateTime.parse("2026-09-04T14:20"), "QUICK", (short) 2, List.of("fear"), "I was scared.");
        var built = CheckinPromptBuilder.build("2026-09", List.of("Plan: 300.00 USD per month, buy day 1 (due 2026-09-01).", "Status: DONE."),
                List.of(entry), true, 20000);
        when(checkinAi.buildPrompt("2026-09", true)).thenReturn(built);
        MvcResult result = mvc.perform(get("/api/v1/invest/ai/context").param("feature", "checkin").param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature").value("checkin"))
                .andExpect(jsonPath("$.snippets").value(1))
                .andExpect(jsonPath("$.systemPrompt").value(org.hamcrest.Matchers.containsString("Task: write a short summary")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("- Plan: 300.00 USD per month")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("[J12]")))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(SECRET));
        verify(llm, org.mockito.Mockito.never()).completeWithUsage(any(), any(), any()); // a preview sends nothing
    }

    @Test
    void theCheckinPreviewNeedsAMonthAndHonoursTheJournalSwitch() throws Exception {
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "checkin")).andExpect(status().isBadRequest());
        var built = CheckinPromptBuilder.build("2026-09", List.of("Status: DONE."), List.of(), false, 20000);
        when(checkinAi.buildPrompt("2026-09", false)).thenReturn(built);
        mvc.perform(get("/api/v1/invest/ai/context").param("feature", "checkin").param("month", "2026-09").param("includeJournal", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includeJournal").value(false))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("chose not to share their journal")));
    }
}
