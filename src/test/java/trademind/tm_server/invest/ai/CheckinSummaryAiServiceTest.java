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
import trademind.tm_server.invest.ai.AiJsonServiceTest.ScriptedLlm;
import trademind.tm_server.invest.ai.AiRateLimiterTest.MutableClock;
import trademind.tm_server.invest.checkin.CheckinFactsService;
import trademind.tm_server.invest.checkin.CheckinSummaryResponse;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.rulecheck.RuleCheckService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The check-in summary with a scripted model against real data. Clock: 2026-10-06 UTC; plan from 2026-08-01, buy day 1. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CheckinSummaryAiServiceTest.TestApp.class)
@Import({CheckinSummaryAiService.class, CheckinFactsService.class, RuleCheckService.class, AiJsonService.class, AiService.class,
        ContributionService.class, CheckinSummaryAiServiceTest.Wiring.class})
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
                + "classpath:db/migration/V29__invest_settings_changes.sql",
        "ai.enabled=true",
        "ai.limits.per-minute=1000",
        "ai.provider.api-key=test-key",
        "ai.provider.model=claude-test"
})

class CheckinSummaryAiServiceTest {

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

    @Autowired CheckinSummaryAiService service;
    @Autowired CheckinFactsService facts;
    @Autowired JdbcTemplate jdbc;
    @Autowired ScriptedLlm llm;
    @Autowired InMemoryAiUsageStore store;
    @Autowired InvestSettingsRepository settings;

    private long sepBuy;

    @BeforeEach
    void setUp() {
        llm.script.clear();
        llm.users.clear();
        store.clear();
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
        buy("2026-08-01", 30000, "3", "90.0000");
        sepBuy = buy("2026-09-05", 30000, "3", "100.0000");
    }

    private long buy(String date, long cents, String shares, String price) {
        return jdbc.queryForObject("INSERT INTO invest_contributions (contributed_on, amount_cents, shares, price) "
                + "VALUES (?::date, ?, ?::numeric, ?::numeric) RETURNING id", Long.class, date, cents, shares, price);
    }

    private long entry(String createdAt, String body, Integer mood, String... tags) {
        long id = jdbc.queryForObject("INSERT INTO invest_journal_entries (entry_type, mood, body, created_at) "
                + "VALUES ('QUICK', ?, ?, ?::timestamp) RETURNING id", Long.class, mood, body, createdAt);
        for (String tag : tags) {
            Long tagId = jdbc.query("SELECT id FROM invest_tags WHERE name = ?", rs -> rs.next() ? rs.getLong(1) : null, tag);
            if (tagId == null) tagId = jdbc.queryForObject("INSERT INTO invest_tags (name) VALUES (?) RETURNING id", Long.class, tag);
            jdbc.update("INSERT INTO invest_journal_entry_tags (entry_id, tag_id) VALUES (?, ?)", id, tagId);
        }
        return id;
    }

    private static String reply(String text, String sources) {
        return "{\"text\":\"" + text.replace("\n", "\\n") + "\",\"sources\":" + sources + "}";
    }

    private static final String GOOD_TEXT = "**What happened**\nThe 300.00 USD buy came 4 days after the buy day.\n"
            + "**How it felt**\nThe owner was scared and wrote about waiting 72 hours.";

    @Test
    void withoutAskingForADraftTheFactsAreReturnedAndNoModelIsCalled() {
        entry("2026-09-04 14:20", "I was scared.", 2, "fear");
        CheckinSummaryResponse r = service.summarize("2026-09", false, true);
        assertEquals("CHECKIN_SUMMARY", r.kind());
        assertEquals("2026-09", r.month());
        assertNull(r.text());
        assertFalse(r.model().requested());
        assertEquals(sepBuy, r.contributionId());
        assertEquals(30000, r.facts().investedCents());
        assertTrue(r.factsLines().contains("Timing: first buy 4 days after the buy day."), r.factsLines().toString());
        assertEquals(0, llm.users.size());
    }

    @Test
    void aDraftThatPassesEveryCheckComesBackWithItsSources() {
        long scared = entry("2026-09-04 14:20", "I was scared and nearly waited 72 hours.", 2, "fear");
        llm.reply(reply(GOOD_TEXT, "[\"J" + scared + "\",\"J999\"]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        assertTrue(r.model().requested() && r.model().ran());
        assertNull(r.model().reason());
        assertEquals(GOOD_TEXT, r.text());
        assertEquals(List.of(scared), r.sources(), "an unknown handle is dropped");
        assertEquals(1, r.model().entriesSent());
        assertEquals(0, r.model().entriesSkipped());
        assertEquals(30000, r.facts().investedCents(), "the facts are the code's, whatever the model wrote");
    }

    @Test
    void theModelIsGivenTheFactsAndTheMonthsEntriesAndNothingFromOtherMonths() {
        long in = entry("2026-09-04 14:20", "September thought.", 2);
        entry("2026-08-31 23:00", "August thought.", 3);
        entry("2026-10-01 01:00", "October thought.", 3);
        llm.reply(reply("**What happened**\nA buy was made.", "[]"));
        service.summarize("2026-09", true, true);
        String sent = llm.users.get(0);
        assertTrue(sent.contains("- Bought: 2026-09-05, 300.00 USD for 3 shares at 100.0000 (fee 0.00 USD)."), sent);
        assertTrue(sent.contains("[J" + in + "]"));
        assertFalse(sent.contains("August thought"));
        assertFalse(sent.contains("October thought"));
    }

    @Test
    void aNumberTheCodeDidNotComputeCausesOneRepeatThatNamesIt() {
        entry("2026-09-04 14:20", "I was scared and nearly waited 72 hours.", 2); // the owner's own number may be used
        llm.reply(reply("**What happened**\nThe buy of 350.00 USD was late.", "[]"));
        llm.reply(reply(GOOD_TEXT, "[]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        assertTrue(r.model().ran());
        assertEquals(GOOD_TEXT, r.text());
        assertEquals(2, llm.users.size());
        assertTrue(llm.users.get(1).contains("the text used numbers that are not in the Facts: 350.00"), llm.users.get(1));
        assertFalse(llm.users.get(1).contains("The buy of 350.00 USD was late"), "the bad draft is not echoed back");
    }

    @Test
    void twoBadDraftsGiveTheFactsWithoutProseAndTheReason() {
        llm.reply(reply("Up 10.7% this month.", "[]"));
        llm.reply(reply("Still 10.7% up.", "[]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        assertNull(r.text());
        assertTrue(r.model().requested());
        assertFalse(r.model().ran());
        assertTrue(r.model().reason().contains("could not be used"), r.model().reason());
        assertEquals(30000, r.facts().investedCents());
        assertFalse(r.factsLines().isEmpty());
        assertEquals(2, llm.users.size());
    }

    @Test
    void adviceInADraftIsNeverShown() {
        llm.reply(reply("You should buy more next month.", "[]"));
        llm.reply(reply("You should buy more next month.", "[]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        assertNull(r.text());
        assertFalse(r.model().ran());
    }

    @Test
    void aNumberOnlyTheOwnersEntryHadIsRejectedWhenTheJournalWasNotShared() {
        entry("2026-09-04 14:20", "I nearly waited 72 hours.", 2);
        llm.reply(reply(GOOD_TEXT, "[]"));
        llm.reply(reply(GOOD_TEXT, "[]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, false);
        assertNull(r.text(), "72 is not in the facts and the entry was not sent");
        assertTrue(llm.users.get(1).contains("numbers that are not in the Facts: 72"));
    }

    @Test
    void withoutTheJournalNoEntryTextIsSentAndNoSourcesAreReturned() {
        entry("2026-09-04 14:20", "A very private thought.", 2);
        llm.reply(reply("**What happened**\nA buy was made.", "[\"J1\"]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, false);
        assertTrue(r.model().ran());
        assertFalse(llm.users.get(0).contains("A very private thought"));
        assertTrue(llm.users.get(0).contains("The owner chose not to share their journal for this summary."));
        assertEquals(0, r.model().entriesSent());
        assertEquals(List.of(), r.sources());
    }

    @Test
    void onlyTheNewestTwentyEntriesAreSent() {
        for (int i = 1; i <= 25; i++) entry("2026-09-" + String.format("%02d", i) + " 10:00", "Entry number " + i, null);
        llm.reply(reply("**What happened**\nA buy was made.", "[]"));
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        assertEquals(20, r.model().entriesSent());
        assertEquals(5, r.model().entriesSkipped());
        assertEquals(25, r.facts().entryCount(), "the facts still count every entry");
    }

    @Test
    void thePreviewIsExactlyWhatIsSent() {
        entry("2026-09-04 14:20", "I was scared.", 2, "fear");
        llm.reply(reply("**What happened**\nA buy was made.", "[]"));
        service.summarize("2026-09", true, true);
        CheckinPromptBuilder.Built preview = service.buildPrompt("2026-09", true);
        assertEquals(llm.users.get(0), preview.prompt().user());
        assertEquals(CheckinPromptBuilder.systemPrompt(), preview.prompt().system());
    }

    // ---- when the draft cannot be written, the facts still come back ---------------------------

    private void expectFactsOnly(CheckinSummaryResponse r) {
        assertNull(r.text());
        assertTrue(r.model().requested());
        assertFalse(r.model().ran());
        assertEquals(30000, r.facts().investedCents());
    }

    @Test
    void aFailedCallIsAReasonNotAnError() {
        llm.fail();
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        expectFactsOnly(r);
        assertEquals("The AI service did not answer. The numbers above are still correct.", r.model().reason());
    }

    @Test
    void aReachedLimitIsAReasonNotAnError() {
        for (int i = 0; i < 60; i++) store.recordCall(LocalDate.parse("2026-10-06"), AiFeature.CHAT);
        CheckinSummaryResponse r = service.summarize("2026-09", true, true);
        expectFactsOnly(r);
        assertTrue(r.model().reason().contains("daily AI limit"), r.model().reason());
        assertEquals(0, llm.users.size());
    }

    @Test
    void whenAiIsSwitchedOffTheFactsStillWork() {
        AiProperties off = new AiProperties(false, new AiProperties.Limits(60, 100, 20_000));
        AiService offAi = new AiService(llm, store, off, new MutableClock(), "k", "m");
        CheckinSummaryAiService offService = new CheckinSummaryAiService(facts, new AiJsonService(offAi), offAi, off);
        CheckinSummaryResponse r = offService.summarize("2026-09", true, true);
        expectFactsOnly(r);
        assertEquals("AI is switched off. Set ai.enabled=true on the server", r.model().reason());
        assertEquals(0, llm.users.size());
    }

    @Test
    void aBadMonthIsABadRequestAndNothingIsSent() {
        assertThrows(BadRequestException.class, () -> service.summarize("2026-9", true, true));
        assertThrows(BadRequestException.class, () -> service.summarize("2026-07", true, true));
        assertThrows(BadRequestException.class, () -> service.summarize("2026-11", true, true));
        assertThrows(BadRequestException.class, () -> service.buildPrompt("nope", true));
        assertEquals(0, llm.users.size());
    }

    @Test
    void summarisingChangesNothing() {
        long e = entry("2026-09-04 14:20", "I was scared and nearly waited 72 hours.", 2, "fear");
        llm.reply(reply(GOOD_TEXT, "[\"J" + e + "\"]"));
        String counts = "SELECT (SELECT count(*) FROM invest_contributions) + (SELECT count(*) FROM invest_journal_entries) "
                + "+ (SELECT count(*) FROM invest_tags) + (SELECT count(*) FROM invest_journal_entry_tags) + (SELECT count(*) FROM invest_rules)";
        long before = jdbc.queryForObject(counts, Long.class);
        assertNotNull(service.summarize("2026-09", true, true).text());
        assertEquals(before, jdbc.queryForObject(counts, Long.class));
    }
}
