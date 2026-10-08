package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trademind.tm_server.ai.LlmClient;
import trademind.tm_server.ai.LlmException;
import trademind.tm_server.ai.LlmOptions;
import trademind.tm_server.ai.LlmResult;
import trademind.tm_server.invest.ai.AiRateLimiterTest.MutableClock;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The JSON call: parse, repair once, then fail with a typed error. A scripted model plays the replies in order. */
class AiJsonServiceTest {

    record Finding(long entryId, String quote) {
    }

    record Report(String summary, List<Finding> findings) {
    }

    static final String GOOD = """
            {"summary":"ok","findings":[{"entryId":12,"quote":"I was scared"},{"entryId":999,"quote":"made up"}]}""";

    /** Plays one scripted answer per call and remembers what it was asked. */
    static class ScriptedLlm implements LlmClient {
        final Deque<Supplier<LlmResult>> script = new ArrayDeque<>();
        final List<String> users = new ArrayList<>();
        final List<LlmOptions> options = new ArrayList<>();

        ScriptedLlm reply(String text) {
            return reply(text, "end_turn");
        }

        ScriptedLlm reply(String text, String stop) {
            script.add(() -> new LlmResult(text, 100, 20, stop));
            return this;
        }

        ScriptedLlm fail() {
            script.add(() -> {
                throw new LlmException("boom with a private prompt echo");
            });
            return this;
        }

        @Override
        public String complete(String system, String user) {
            return completeWithUsage(system, user, LlmOptions.defaults()).text();
        }

        @Override
        public LlmResult completeWithUsage(String system, String user, LlmOptions o) {
            users.add(user);
            options.add(o);
            return script.removeFirst().get();
        }
    }

    ScriptedLlm llm;
    InMemoryAiUsageStore store;

    @BeforeEach
    void setUp() {
        llm = new ScriptedLlm();
        store = new InMemoryAiUsageStore();
    }

    private AiJsonService json(int daily, int perMinute, int maxChars) {
        AiProperties props = new AiProperties(true, new AiProperties.Limits(daily, perMinute, maxChars));
        return new AiJsonService(new AiService(llm, store, props, new MutableClock(), "k", "m"));
    }

    private AiJsonService json() {
        return json(60, 100, 20_000);
    }

    private static final java.util.function.UnaryOperator<Report> KEEP = r -> r;

    /** The kind of cleaning a real feature does: only ids that were shown, only quotes that are really there. */
    private static Report clean(Report r) {
        Set<Long> shown = Set.of(12L);
        List<String> sources = List.of("I was scared when the market dropped.");
        List<Finding> kept = new ArrayList<>();
        for (Finding f : r.findings()) {
            if (!KnownIds.isKnown(f.entryId(), shown)) continue;
            kept.add(new Finding(f.entryId(), QuoteCheck.verified(f.quote(), sources).orElse(null)));
        }
        return new Report(r.summary(), kept);
    }

    private long calls() {
        return store.totals(LocalDate.parse("2026-10-06")).calls();
    }

    @Test
    void aUsableFirstReplyIsReturnedWithoutARepeat() {
        llm.reply(GOOD);
        AiJsonService.Result<Report> r = json().call(AiFeature.RULE_CHECK, "sys", "user", Report.class, KEEP);
        assertEquals("ok", r.value().summary());
        assertFalse(r.repaired());
        assertEquals(1, llm.users.size());
        assertEquals(1, calls());
        assertEquals(100, r.inputTokens());
        assertEquals(20, r.outputTokens());
    }

    @Test
    void theCheckCleansTheValueBeforeItIsReturned() {
        llm.reply(GOOD);
        Report r = json().call(AiFeature.RULE_CHECK, "sys", "user", Report.class, AiJsonServiceTest::clean).value();
        assertEquals(List.of(new Finding(12, "I was scared")), r.findings(), "the made-up id 999 is dropped, the real quote kept");
    }

    @Test
    void anUnusableReplyIsRepeatedOnceWithTheReasonAndTheOriginalPrompt() {
        llm.reply("Sure! Here you go: {\"summary\":\"ok\"}").reply(GOOD);
        AiJsonService.Result<Report> r = json().call(AiFeature.RULE_CHECK, "sys", "ORIGINAL USER PROMPT", Report.class, KEEP);
        assertTrue(r.repaired());
        assertEquals(2, llm.users.size());
        assertEquals("ORIGINAL USER PROMPT", llm.users.get(0));
        String repair = llm.users.get(1);
        assertTrue(repair.startsWith("ORIGINAL USER PROMPT"), "the task is not lost");
        assertTrue(repair.contains("## Problem with your previous reply"));
        assertTrue(repair.contains("it was not valid JSON, or had text around it"));
        assertTrue(repair.contains("only the JSON object described above"));
        assertFalse(repair.contains("Sure! Here you go"), "the bad reply is not echoed back into the prompt");
        assertEquals(2, calls(), "the repeat is a real call and counts");
        assertEquals(200, r.inputTokens(), "tokens of both calls are added up");
        assertEquals(40, r.outputTokens());
    }

    @Test
    void aMissingFieldIsNamedInTheRepair() {
        llm.reply("{\"summary\":\"x\"}").reply(GOOD);
        json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP);
        assertTrue(llm.users.get(1).contains("the field 'findings' is missing"));
    }

    @Test
    void twoUnusableRepliesGiveATypedErrorAndNoReplyTextEscapes() {
        llm.reply("SECRET-REPLY-ONE").reply("{\"summary\": \"SECRET-REPLY-TWO\"");
        AiInvalidReplyException e = assertThrows(AiInvalidReplyException.class,
                () -> json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP));
        assertFalse(e.getMessage().contains("SECRET"));
        assertEquals(2, llm.users.size(), "exactly one repeat, never more");
        assertEquals(2, calls());
        assertFalse(llm.users.get(1).contains("SECRET"));
    }

    @Test
    void aReplyCutOffAtTheLimitIsNotTrustedEvenIfItParses() {
        llm.reply(GOOD, "max_tokens").reply(GOOD);
        AiJsonService.Result<Report> r = json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP);
        assertTrue(r.repaired());
        String repair = llm.users.get(1);
        assertTrue(repair.contains("the reply was cut off at the length limit"));
        assertTrue(repair.contains("Keep it shorter this time."));
    }

    @Test
    void aSecondReplyCutOffToIsAnError() {
        llm.reply(GOOD, "max_tokens").reply(GOOD, "max_tokens");
        assertThrows(AiInvalidReplyException.class, () -> json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP));
    }

    @Test
    void aCheckThatRejectsTheValueAsksForARepeat() {
        llm.reply(GOOD).reply(GOOD);
        int[] seen = {0};
        AiJsonService.Result<Report> r = json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, v -> {
            if (seen[0]++ == 0) throw new ModelJsonException("every finding named an entry that was not shown");
            return v;
        });
        assertTrue(r.repaired());
        assertTrue(llm.users.get(1).contains("every finding named an entry that was not shown"));
    }

    @Test
    void aCheckThatReturnsNothingCountsAsUnusable() {
        llm.reply(GOOD).reply(GOOD);
        assertThrows(AiInvalidReplyException.class, () -> json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, v -> null));
        assertEquals(2, llm.users.size());
    }

    @Test
    void aFailedCallIsNotRepeatedHereBecauseThatIsNotTheModelsFormatting() {
        llm.fail().reply(GOOD);
        assertThrows(AiUpstreamException.class, () -> json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP));
        assertEquals(1, llm.users.size());
    }

    @Test
    void aFailureOnTheRepeatSurfacesAsTheUsualUpstreamError() {
        llm.reply("nope").fail();
        assertThrows(AiUpstreamException.class, () -> json().call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP));
        assertEquals(2, calls());
    }

    @Test
    void theRepeatRespectsTheCaps() {
        llm.reply("nope").reply(GOOD);
        AiJsonService limited = json(1, 100, 20_000); // one call a day
        AiLimitExceededException e = assertThrows(AiLimitExceededException.class,
                () -> limited.call(AiFeature.RULE_CHECK, "s", "u", Report.class, KEEP));
        assertTrue(e.getMessage().contains("daily"));
        assertEquals(1, llm.users.size(), "the repeat was never sent");
    }

    @Test
    void ifTheRepairNoteWouldPushThePromptOverTheSizeCapItIsAnInvalidReplyNotABadRequest() {
        llm.reply("nope").reply(GOOD);
        // 3 + 90 characters fit a cap of 100; the repair note does not.
        AiJsonService tight = json(60, 100, 100);
        assertThrows(AiInvalidReplyException.class,
                () -> tight.call(AiFeature.RULE_CHECK, "sys", "u".repeat(90), Report.class, KEEP));
        assertEquals(1, llm.users.size());
    }

    @Test
    void theFeaturesOwnReplyBudgetIsUsedForBothCalls() {
        llm.reply("nope").reply(GOOD);
        json().call(AiFeature.TAG_SUGGESTIONS, "s", "u", Report.class, KEEP);
        assertEquals(List.of(AiFeature.TAG_SUGGESTIONS.options(), AiFeature.TAG_SUGGESTIONS.options()), llm.options);
    }
}
