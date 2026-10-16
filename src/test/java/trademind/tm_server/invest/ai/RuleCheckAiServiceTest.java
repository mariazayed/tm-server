package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.ai.AiRateLimiterTest.MutableClock;
import trademind.tm_server.invest.ai.AiJsonServiceTest.ScriptedLlm;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.rulecheck.RuleCheckResponse;
import trademind.tm_server.invest.rulecheck.RuleCheckService;
import trademind.tm_server.invest.rulecheck.RuleFinding;
import trademind.tm_server.invest.rules.RulesService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole consistency check with a scripted model against real data: rules, entries, tags, soft deletes. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RuleCheckAiServiceTest.TestApp.class)
@Import({RuleCheckAiService.class, RuleCheckService.class, AiJsonService.class, AiService.class, RulesService.class,
        ContributionService.class, RuleCheckAiServiceTest.Wiring.class})
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.datasource.hikari.maximum-pool-size=2", // every cached test context holds its own pool; the default 10 each exhausts max_connections=100
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:invest-schema-reset.sql,"
                + "classpath:db/migration/V22__create_invest_tables.sql,"
                + "classpath:db/migration/V23__create_invest_flashcards.sql,"
                + "classpath:db/migration/V24__invest_reminders.sql,"
                + "classpath:db/migration/V25__invest_embeddings.sql,"
                + "classpath:db/migration/V26__invest_ai_usage.sql,"
                + "classpath:db/migration/V27__invest_fulltext_search.sql,"
                + "classpath:db/migration/V28__invest_rule_wait_hours.sql,"
                + "classpath:db/migration/V29__invest_settings_changes.sql,"
                + "classpath:db/migration/V30__create_invest_holdings.sql",
        "ai.enabled=true",
        "ai.limits.per-minute=1000",
        "ai.limits.daily-total=60",
        "ai.provider.api-key=test-key",
        "ai.provider.model=claude-test"
})
class RuleCheckAiServiceTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    /** Not @Configuration on purpose: InvestEndToEndTest scans this package. */
    @EnableConfigurationProperties(AiProperties.class)
    static class Wiring {
        @Bean
        Clock clock() {
            return new MutableClock();
        }

        @Bean
        ScriptedLlm llm() {
            return new ScriptedLlm();
        }

        @Bean
        InMemoryAiUsageStore usageStore() {
            return new InMemoryAiUsageStore();
        }
    }

    @Autowired RuleCheckAiService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired ScriptedLlm llm;
    @Autowired InMemoryAiUsageStore store;
    @Autowired InvestSettingsRepository settings;

    private static final LocalDate FROM = LocalDate.parse("2026-10-01");
    private static final LocalDate TO = LocalDate.parse("2026-10-15");

    private long rule1;
    private long rule3;

    @BeforeEach
    void setUp() {
        llm.script.clear();
        llm.users.clear();
        store.clear();
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-10-01")); // no missed months to confuse the exact checks
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
        List<Long> ids = jdbc.queryForList("SELECT id FROM invest_rules WHERE deleted_at IS NULL ORDER BY sort_order", Long.class);
        rule1 = ids.get(0);
        rule3 = ids.get(2);
    }

    private long entry(String createdAt, String body, String... tags) {
        long id = jdbc.queryForObject("INSERT INTO invest_journal_entries (entry_type, body, created_at) "
                + "VALUES ('QUICK', ?, ?::timestamp) RETURNING id", Long.class, body, createdAt);
        for (String tag : tags) {
            Long tagId = jdbc.query("SELECT id FROM invest_tags WHERE name = ?", rs -> rs.next() ? rs.getLong(1) : null, tag);
            if (tagId == null) tagId = jdbc.queryForObject("INSERT INTO invest_tags (name) VALUES (?) RETURNING id", Long.class, tag);
            jdbc.update("INSERT INTO invest_journal_entry_tags (entry_id, tag_id) VALUES (?, ?)", id, tagId);
        }
        return id;
    }

    private static String reply(String rule, String entries, String reason, String quote, String confidence) {
        return "{\"findings\":[{\"rule\":\"" + rule + "\",\"entries\":" + entries + ",\"reason\":\"" + reason + "\",\"quote\":"
                + (quote == null ? "null" : "\"" + quote + "\"") + ",\"confidence\":\"" + confidence + "\"}]}";
    }

    private RuleCheckResponse withModel() {
        return service.run(FROM, TO);
    }

    @Test
    void aVerifiedFindingComesBackWithTheRuleItsEntriesAndItsQuote() {
        long sold = entry("2026-10-05 10:00", "I sold everything in the crash because I panicked.", "fear");
        llm.reply(reply("R1", "[\"J" + sold + "\"]", "The entry records selling during a crash.", "sold everything in the crash", "HIGH"));

        RuleCheckResponse r = withModel();
        assertTrue(r.model().requested() && r.model().ran());
        assertNull(r.model().reason());
        assertEquals(1, r.model().entriesChecked());
        assertEquals(0, r.model().entriesSkipped());
        assertEquals(0, r.model().dropped());
        RuleFinding f = r.findings().stream().filter(x -> x.source().equals("MODEL")).findFirst().orElseThrow();
        assertEquals("MODEL_REVIEW", f.check());
        assertEquals(rule1, f.ruleId());
        assertEquals(List.of(sold), f.entryIds());
        assertEquals("sold everything in the crash", f.quote());
        assertEquals("HIGH", f.confidence());
        assertEquals(LocalDate.parse("2026-10-05"), f.date());
        assertEquals("The entry records selling during a crash.", f.message());
    }

    @Test
    void theModelSeesTheRulesAndOnlyLiveEntriesInTheRange() {
        long live = entry("2026-10-05 10:00", "Live entry text.");
        long gone = entry("2026-10-06 10:00", "Deleted entry text.");
        entry("2026-09-30 23:59", "Before the range text.");
        entry("2026-10-16 00:00", "After the range text.");
        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now() WHERE id = ?", gone);
        llm.reply("{\"findings\":[]}");
        withModel();

        String sent = llm.users.get(0);
        assertTrue(sent.contains("[J" + live + "]"));
        assertTrue(sent.contains("[R1] <user_text>Never sell in a crash.</user_text>"));
        assertTrue(sent.contains("[R3]"));
        assertFalse(sent.contains("Deleted entry text"));
        assertFalse(sent.contains("Before the range text"));
        assertFalse(sent.contains("After the range text"));
    }

    @Test
    void thePreviewIsExactlyWhatIsSent() {
        entry("2026-10-05 10:00", "Some text.", "decision");
        llm.reply("{\"findings\":[]}");
        withModel();
        RuleCheckPromptBuilder.Built preview = service.buildPrompt(FROM, TO);
        assertEquals(llm.users.get(0), preview.prompt().user());
        assertEquals(RuleCheckPromptBuilder.systemPrompt(), preview.prompt().system());
    }

    @Test
    void anInventedRuleEntryOrQuoteIsDroppedOrDowngradedNeverShown() {
        long sold = entry("2026-10-05 10:00", "I sold everything in the crash because I panicked.");
        llm.reply("{\"findings\":["
                + "{\"rule\":\"R9\",\"entries\":[\"J" + sold + "\"],\"reason\":\"Unknown rule.\",\"quote\":null,\"confidence\":\"HIGH\"},"
                + "{\"rule\":\"R1\",\"entries\":[\"J99999\"],\"reason\":\"Unknown entry.\",\"quote\":null,\"confidence\":\"HIGH\"},"
                + "{\"rule\":\"R1\",\"entries\":[\"J" + sold + "\"],\"reason\":\"Sold in a crash.\",\"quote\":\"I sold at the very bottom\",\"confidence\":\"HIGH\"}]}");
        RuleCheckResponse r = withModel();
        List<RuleFinding> model = r.findings().stream().filter(x -> x.source().equals("MODEL")).toList();
        assertEquals(1, model.size());
        assertNull(model.get(0).quote(), "an invented quote is dropped");
        assertEquals("MEDIUM", model.get(0).confidence(), "HIGH needs a verified quote");
        assertEquals(2, r.model().dropped());
    }

    @Test
    void adviceInAReasonDropsTheFinding() {
        long sold = entry("2026-10-05 10:00", "I sold in the crash.");
        llm.reply(reply("R1", "[\"J" + sold + "\"]", "You should buy back in now.", null, "MEDIUM"));
        llm.reply(reply("R1", "[\"J" + sold + "\"]", "You should buy back in now.", null, "MEDIUM"));
        RuleCheckResponse r = withModel();
        assertFalse(r.model().ran(), "every finding was unusable twice: the exact result still stands");
        assertEquals("The AI answered in a form that could not be used. The exact checks above are still valid.", r.model().reason());
        assertEquals(2, llm.users.size(), "one repeat, no more");
    }

    @Test
    void aFirstReplyWithOnlyInventedHandlesIsRepairedOnce() {
        long sold = entry("2026-10-05 10:00", "I sold everything in the crash.");
        llm.reply(reply("R9", "[\"J1\"]", "Made up.", null, "HIGH"));
        llm.reply(reply("R1", "[\"J" + sold + "\"]", "Sold in a crash.", "sold everything in the crash", "HIGH"));
        RuleCheckResponse r = withModel();
        assertTrue(r.model().ran());
        assertEquals(1, r.findings().stream().filter(x -> x.source().equals("MODEL")).count());
        assertTrue(llm.users.get(1).contains("every finding named a rule or entry that was not in the lists"));
    }

    @Test
    void noConflictsFoundIsAValidAnswer() {
        entry("2026-10-05 10:00", "Calm month.");
        llm.reply("{\"findings\":[]}");
        RuleCheckResponse r = withModel();
        assertTrue(r.model().ran());
        assertEquals(List.of(), r.findings());
    }

    @Test
    void modelAndExactFindingsAreMergedNewestFirst() {
        long dev = entry("2026-10-03 09:00", "Deviated.", "deviation"); // exact: no decision entry before it
        long sold = entry("2026-10-09 10:00", "I sold everything in the crash.");
        llm.reply(reply("R3", "[\"J" + sold + "\"]", "Sold in a crash.", null, "LOW"));
        RuleCheckResponse r = withModel();
        assertEquals(List.of("MODEL", "DETERMINISTIC"), r.findings().stream().map(RuleFinding::source).toList());
        assertEquals(List.of(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-03")), r.findings().stream().map(RuleFinding::date).toList());
        assertEquals(rule3, r.findings().get(0).ruleId());
        assertEquals(List.of(dev), r.findings().get(1).entryIds());
    }

    // ---- when the model part cannot run, the exact part is still returned ----------------------

    private void expectExactStillThere(RuleCheckResponse r) {
        assertTrue(r.model().requested());
        assertFalse(r.model().ran());
        assertEquals(3, r.checks().size());
        assertTrue(r.findings().stream().allMatch(f -> f.source().equals("DETERMINISTIC")));
    }

    @Test
    void whenTheModelCannotAnswerTheExactFindingsAreStillReturnedWithTheReason() {
        entry("2026-10-03 09:00", "Deviated.", "deviation");
        llm.fail();
        RuleCheckResponse r = withModel();
        expectExactStillThere(r);
        assertEquals(1, r.findings().size());
        assertEquals("The AI service did not answer. The exact checks above are still valid.", r.model().reason());
    }

    @Test
    void aReachedLimitIsAReasonNotAnError() {
        entry("2026-10-03 09:00", "Some text.");
        for (int i = 0; i < 60; i++) store.recordCall(LocalDate.parse("2026-10-06"), AiFeature.CHAT);
        RuleCheckResponse r = withModel();
        expectExactStillThere(r);
        assertTrue(r.model().reason().contains("daily AI limit"), r.model().reason());
        assertEquals(0, llm.users.size());
    }

    @Test
    void withNoRulesOrNoEntriesTheModelIsNotCalled() {
        entry("2026-10-03 09:00", "Some text.");
        jdbc.update("UPDATE invest_rules SET deleted_at = now()");
        RuleCheckResponse noRules = withModel();
        expectExactStillThere(noRules);
        assertTrue(noRules.model().reason().contains("no rules written yet"));

        jdbc.update("UPDATE invest_rules SET deleted_at = NULL");
        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now()");
        RuleCheckResponse noEntries = withModel();
        expectExactStillThere(noEntries);
        assertEquals("There are no journal entries in this range.", noEntries.model().reason());
        assertEquals(0, llm.users.size());
    }

    @Test
    void onlyTheNewestFortyEntriesAreSentAndTheRestIsCounted() {
        for (int i = 1; i <= 45; i++) entry("2026-10-" + String.format("%02d", (i % 14) + 1) + " " + String.format("%02d", i % 24) + ":" + String.format("%02d", i) + ":00", "Entry number " + i);
        llm.reply("{\"findings\":[]}");
        RuleCheckResponse r = withModel();
        assertEquals(40, r.model().entriesChecked());
        assertEquals(5, r.model().entriesSkipped());
    }

    @Test
    void aBadRangeIsStillABadRequestBecauseThatIsTheCallersMistake() {
        assertThrows(BadRequestException.class, () -> service.run(TO, FROM));
        assertThrows(BadRequestException.class, () -> service.buildPrompt(TO, FROM));
        assertEquals(0, llm.users.size());
    }

    @Test
    void theCheckChangesNothing() {
        long sold = entry("2026-10-05 10:00", "I sold everything in the crash.", "fear");
        llm.reply(reply("R1", "[\"J" + sold + "\"]", "Sold in a crash.", null, "MEDIUM"));
        String counts = "SELECT (SELECT count(*) FROM invest_journal_entries) + (SELECT count(*) FROM invest_rules) "
                + "+ (SELECT count(*) FROM invest_tags) + (SELECT count(*) FROM invest_settings_changes) + (SELECT count(*) FROM invest_journal_entry_tags)";
        long before = jdbc.queryForObject(counts, Long.class);
        withModel();
        assertEquals(before, jdbc.queryForObject(counts, Long.class));
    }
}
