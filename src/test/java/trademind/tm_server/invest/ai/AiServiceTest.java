package trademind.tm_server.invest.ai;

import trademind.tm_server.exceptions.BadRequestException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import trademind.tm_server.ai.LlmClient;
import trademind.tm_server.ai.LlmException;
import trademind.tm_server.ai.LlmOptions;
import trademind.tm_server.ai.LlmResult;
import trademind.tm_server.invest.ai.AiRateLimiterTest.MutableClock;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiServiceTest {

    static final String PROMPT_TEXT = "my private journal sentence about fear";
    static final String REPLY_TEXT = "a private model reply";

    /** Scripted model: records what it was called with and can fail. */
    static class FakeLlm implements LlmClient {
        final List<LlmOptions> options = new ArrayList<>();
        int calls;
        String lastSystem;
        String lastUser;
        RuntimeException failure;
        LlmResult reply = new LlmResult(REPLY_TEXT, 100, 20, "end_turn");

        @Override public String complete(String system, String user) { return reply.text(); }

        @Override
        public LlmResult completeWithUsage(String system, String user, LlmOptions o) {
            calls++;
            lastSystem = system;
            lastUser = user;
            options.add(o);
            if (failure != null) throw failure;
            return reply;
        }
    }

    FakeLlm llm;
    InMemoryAiUsageStore store;
    MutableClock clock;
    ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        llm = new FakeLlm();
        store = new InMemoryAiUsageStore();
        clock = new MutableClock();
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(AiService.class)).addAppender(logs);
        ((Logger) LoggerFactory.getLogger(AiService.class)).setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(AiService.class)).detachAppender(logs);
    }

    AiService service(boolean enabled, String key, String model, int daily, int perMinute, int maxChars) {
        return new AiService(llm, store, new AiProperties(enabled, new AiProperties.Limits(daily, perMinute, maxChars)),
                clock, key, model);
    }

    AiService ready() {
        return service(true, "k", "m", 60, 6, 20_000);
    }

    @Test
    void ollamaIsConfiguredWithoutAKey() {
        AiProperties p = new AiProperties(true, new AiProperties.Limits(60, 6, 20_000));
        AiService ollama = new AiService(llm, store, p, clock, "", "ollama", "qwen2.5:7b-instruct");
        assertEquals(new AiController.Status(true, "qwen2.5:7b-instruct", null), ollama.status());
        AiService anthropic = new AiService(llm, store, p, clock, "", "anthropic", "m");
        assertEquals(false, anthropic.status().configured());
    }

    @Test
    void switchedOffMeansNothingIsSent() {
        AiService ai = service(false, "k", "m", 60, 6, 20_000);
        AiUnavailableException e = assertThrows(AiUnavailableException.class, () -> ai.call(AiFeature.CHAT, "s", "u"));
        assertEquals("AI is switched off. Set ai.enabled=true on the server", e.getMessage());
        assertEquals(0, llm.calls);
        assertEquals(0, store.totals(LocalDate.parse("2026-10-06")).calls());
    }

    @Test
    void missingKeyOrModelMeansNothingIsSent() {
        assertThrows(AiUnavailableException.class, () -> service(true, "", "m", 60, 6, 20_000).call(AiFeature.CHAT, "s", "u"));
        assertThrows(AiUnavailableException.class, () -> service(true, "k", " ", 60, 6, 20_000).call(AiFeature.CHAT, "s", "u"));
        assertEquals(0, llm.calls);
    }

    @Test
    void aSuccessfulCallReturnsTheReplyAndCountsCallsAndTokens() {
        LlmResult result = ready().call(AiFeature.RULE_CHECK, "s", "u");
        assertSame(llm.reply, result);
        AiUsageStore.Totals t = store.totals(LocalDate.parse("2026-10-06"));
        assertEquals(1, t.calls());
        assertEquals(100, t.inputTokens());
        assertEquals(20, t.outputTokens());
        assertEquals(1L, t.callsByFeature().get("RULE_CHECK"));
    }

    @Test
    void eachFeatureUsesItsOwnReplyBudgetAndSampling() {
        ready().call(AiFeature.TAG_SUGGESTIONS, "s", "u");
        assertEquals(new LlmOptions(400, 0.2, null), llm.options.get(0));
        ready().call(AiFeature.CHAT, "s", "u");
        assertEquals(new LlmOptions(1024, null, null), llm.options.get(1));
    }

    @Test
    void theDailyCapStopsCallsBeforeTheyAreSentAndResetsTheNextUtcDay() {
        AiService ai = service(true, "k", "m", 2, 100, 20_000);
        ai.call(AiFeature.CHAT, "s", "u");
        ai.call(AiFeature.TAG_SUGGESTIONS, "s", "u");
        AiLimitExceededException e = assertThrows(AiLimitExceededException.class, () -> ai.call(AiFeature.CHAT, "s", "u"));
        assertEquals(2, llm.calls);
        assertEquals(12 * 3600, e.retryAfterSeconds()); // clock is 12:00 UTC
        assertTrue(e.getMessage().contains("2 calls"));

        clock.advance(Duration.ofHours(12));
        ai.call(AiFeature.CHAT, "s", "u");
        assertEquals(3, llm.calls);
    }

    @Test
    void theDailyCapSurvivesARestart() {
        service(true, "k", "m", 1, 100, 20_000).call(AiFeature.CHAT, "s", "u");
        AiService afterRestart = service(true, "k", "m", 1, 100, 20_000); // new service, same store
        assertThrows(AiLimitExceededException.class, () -> afterRestart.call(AiFeature.CHAT, "s", "u"));
    }

    @Test
    void thePerMinuteCapAsksToWaitAndThenFrees() {
        AiService ai = service(true, "k", "m", 100, 2, 20_000);
        ai.call(AiFeature.CHAT, "s", "u");
        clock.advance(Duration.ofSeconds(20));
        ai.call(AiFeature.CHAT, "s", "u");
        AiLimitExceededException e = assertThrows(AiLimitExceededException.class, () -> ai.call(AiFeature.CHAT, "s", "u"));
        assertEquals(41, e.retryAfterSeconds());
        assertEquals(2, llm.calls);
        clock.advance(Duration.ofSeconds(41));
        ai.call(AiFeature.CHAT, "s", "u");
        assertEquals(3, llm.calls);
    }

    @Test
    void anOversizedPromptIsRefusedBeforeAnythingIsCounted() {
        AiService ai = service(true, "k", "m", 60, 6, 10);
        BadRequestException e = assertThrows(BadRequestException.class, () -> ai.call(AiFeature.CHAT, "123456", "123456"));
        assertEquals("The prompt is too large to send (12 of 10 characters)", e.getMessage());
        assertEquals(0, llm.calls);
        assertEquals(0, store.totals(LocalDate.parse("2026-10-06")).calls());
    }

    @Test
    void aFailedCallStillCountsAndRevealsNothing() {
        llm.failure = new LlmException("HTTP 400: " + PROMPT_TEXT + " sk-ant-LEAKED-VALUE");
        AiUpstreamException e = assertThrows(AiUpstreamException.class, () -> ready().call(AiFeature.CHAT, "s", PROMPT_TEXT));
        assertFalse(e.getMessage().contains(PROMPT_TEXT));
        assertEquals(1, store.totals(LocalDate.parse("2026-10-06")).calls(), "a failed call may still have cost money");
        for (ILoggingEvent event : logs.list) {
            assertFalse(event.getFormattedMessage().contains(PROMPT_TEXT));
            assertFalse(event.getFormattedMessage().contains("LEAKED"));
            assertEquals(null, event.getThrowableProxy(), "no stack trace: it would carry the provider message");
        }
        assertTrue(logs.list.stream().anyMatch(ev -> ev.getFormattedMessage().contains("LlmException")));
    }

    @Test
    void ifTheUsageStoreIsDownNothingIsSent() {
        store.failOnRecord = true;
        assertThrows(IllegalStateException.class, () -> ready().call(AiFeature.CHAT, "s", "u"));
        assertEquals(0, llm.calls, "fail closed: no count, no call");
    }

    @Test
    void logsHoldSizesAndTimingsNeverPromptsOrReplies() {
        ready().call(AiFeature.CHAT, "system text", PROMPT_TEXT);
        assertEquals(1, logs.list.size());
        String line = logs.list.get(0).getFormattedMessage();
        assertTrue(line.contains("feature=CHAT"));
        assertTrue(line.contains("inputTokens=100"));
        assertFalse(line.contains(PROMPT_TEXT));
        assertFalse(line.contains(REPLY_TEXT));
    }

    @Test
    void aTruncatedReplyIsFlaggedForTheCaller() {
        llm.reply = new LlmResult("{\"partial\":", 10, 1024, "max_tokens");
        assertTrue(ready().call(AiFeature.RULE_CHECK, "s", "u").truncated());
    }

    @Test
    void usageReportsTodayAgainstTheCaps() {
        AiService ai = ready();
        ai.call(AiFeature.CHAT, "s", "u");
        AiService.Usage usage = ai.usage();
        assertEquals(LocalDate.parse("2026-10-06"), usage.date());
        assertEquals(1, usage.callsToday());
        assertEquals(60, usage.dailyLimit());
        assertEquals(6, usage.perMinuteLimit());
        assertEquals(100, usage.inputTokens());
    }
}
