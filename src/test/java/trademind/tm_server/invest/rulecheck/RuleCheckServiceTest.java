package trademind.tm_server.invest.rulecheck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The checks against real data: tags, soft deletes, the settings row, real missed months. Fixed clock 2026-10-15 UTC. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RuleCheckServiceTest.TestApp.class)
@Import({ContributionService.class, RuleCheckService.class, RuleCheckServiceTest.FixedClock.class})
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
                + "classpath:db/migration/V30__create_invest_holdings.sql"
})
class RuleCheckServiceTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired RuleCheckService service;
    @Autowired InvestSettingsRepository settings;
    @Autowired JdbcTemplate jdbc;

    private static final LocalDate OCT_1 = LocalDate.parse("2026-10-01");
    private static final LocalDate OCT_15 = LocalDate.parse("2026-10-15");

    private long entry(String createdAt, String... tags) {
        long id = jdbc.queryForObject("INSERT INTO invest_journal_entries (entry_type, body, created_at) "
                + "VALUES ('QUICK', 'x', ?::timestamp) RETURNING id", Long.class, createdAt);
        for (String tag : tags) tag(id, tag);
        return id;
    }

    private void tag(long entryId, String name) {
        Long tagId = jdbc.query("SELECT id FROM invest_tags WHERE name = ?", rs -> rs.next() ? rs.getLong(1) : null, name);
        if (tagId == null) tagId = jdbc.queryForObject("INSERT INTO invest_tags (name) VALUES (?) RETURNING id", Long.class, name);
        jdbc.update("INSERT INTO invest_journal_entry_tags (entry_id, tag_id) VALUES (?, ?)", entryId, tagId);
    }

    private void startPlan(String start) {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse(start));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
    }

    private List<RuleFinding> findings(String check, LocalDate from, LocalDate to) {
        return service.run(from, to).findings().stream().filter(f -> f.check().equals(check)).toList();
    }

    @Test
    void theWaitPeriodUsesRealTagsAndIgnoresDeletedEntriesAndDeletedTags() {
        startPlan("2026-10-01"); // no missed months to confuse the result
        long deletedDecision = entry("2026-09-28 09:00", "decision"); // 4+ days before: would satisfy the wait if it counted
        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now() WHERE id = ?", deletedDecision);
        long decision = entry("2026-10-01 09:00", "decision");
        long deviation = entry("2026-10-02 09:00", "deviation");

        List<RuleFinding> out = findings("WAIT_PERIOD", OCT_1, OCT_15);
        assertEquals(1, out.size());
        assertEquals(List.of(decision, deviation), out.get(0).entryIds());
        assertTrue(out.get(0).message().contains("only 24 hours"), out.get(0).message());

        // A decision tag that has been deleted is no tag at all.
        jdbc.update("UPDATE invest_tags SET deleted_at = now() WHERE name = 'decision'");
        List<RuleFinding> noDecision = findings("WAIT_PERIOD", OCT_1, OCT_15);
        assertTrue(noDecision.get(0).message().contains("no decision entry"), noDecision.get(0).message());
    }

    @Test
    void theWaitComesFromTheSettingsRowAndZeroSwitchesTheCheckOff() {
        startPlan("2026-10-01");
        entry("2026-10-01 09:00", "decision");
        entry("2026-10-02 09:00", "deviation"); // exactly 24 hours

        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setRuleWaitHours(24);
        settings.saveAndFlush(s);
        assertEquals(List.of(), findings("WAIT_PERIOD", OCT_1, OCT_15), "24 hours of waiting meets a 24 hour wait");
        assertEquals(24, service.run(OCT_1, OCT_15).waitHours());

        s.setRuleWaitHours(0);
        settings.saveAndFlush(s);
        RuleCheckService.Result off = service.run(OCT_1, OCT_15);
        assertEquals(List.of(), off.findings());
        RuleCheckService.CheckInfo info = off.checks().get(0);
        assertEquals("WAIT_PERIOD", info.code());
        assertEquals("OFF", info.status());
        assertEquals("Switched off: the wait is set to 0 hours in Settings.", info.note());
    }

    @Test
    void aPlanChangeIsCheckedAgainstTheJournalAndDeletedEntriesDoNotCount() {
        startPlan("2026-10-01");
        jdbc.update("INSERT INTO invest_settings_changes (field_name, old_value, new_value, created_at) "
                + "VALUES ('MONTHLY_AMOUNT', '30000', '40000', '2026-10-10 12:00')");
        long note = entry("2026-10-08 09:00");
        assertEquals(List.of(), findings("PLAN_CHANGE", OCT_1, OCT_15));

        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now() WHERE id = ?", note);
        List<RuleFinding> out = findings("PLAN_CHANGE", OCT_1, OCT_15);
        assertEquals(1, out.size());
        assertTrue(out.get(0).message().contains("monthly amount 300.00 USD to 400.00 USD"), out.get(0).message());
    }

    @Test
    void aDeletedSettingsChangeIsIgnored() {
        startPlan("2026-10-01");
        jdbc.update("INSERT INTO invest_settings_changes (field_name, old_value, new_value, created_at, deleted_at) "
                + "VALUES ('BUY_DAY', '1', '5', '2026-10-10 12:00', now())");
        assertEquals(List.of(), findings("PLAN_CHANGE", OCT_1, OCT_15));
    }

    @Test
    void missedMonthsComeFromTheRealContributionStatus() {
        startPlan("2026-06-01"); // June to September have no purchases
        entry("2026-09-20 10:00"); // an entry in September explains September, but not August
        List<RuleFinding> out = findings("MISSED_MONTH", LocalDate.parse("2026-08-01"), OCT_15);
        assertEquals(1, out.size());
        assertEquals(LocalDate.parse("2026-08-31"), out.get(0).date());
        assertTrue(out.get(0).message().contains("2026-08"), out.get(0).message());
    }

    @Test
    void theResultListsEveryCheckThatRan() {
        startPlan("2026-10-01");
        RuleCheckService.Result r = service.run(OCT_1, OCT_15);
        assertEquals(List.of("WAIT_PERIOD", "PLAN_CHANGE", "MISSED_MONTH"), r.checks().stream().map(RuleCheckService.CheckInfo::code).toList());
        assertEquals(List.of("RAN", "RAN", "RAN"), r.checks().stream().map(RuleCheckService.CheckInfo::status).toList());
        assertNull(r.checks().get(0).note());
        assertEquals(72, r.waitHours());
        assertEquals(OCT_1, r.from());
    }

    @Test
    void findingsComeNewestFirst() {
        startPlan("2026-10-01");
        entry("2026-10-03 09:00", "deviation");
        jdbc.update("INSERT INTO invest_settings_changes (field_name, old_value, new_value, created_at) "
                + "VALUES ('TICKER', 'VT', 'VTI', '2026-10-12 12:00')");
        List<LocalDate> dates = service.run(OCT_1, OCT_15).findings().stream().map(RuleFinding::date).toList();
        assertEquals(List.of(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-03")), dates);
    }

    @Test
    void theRangeIsValidated() {
        assertThrows(BadRequestException.class, () -> service.run(null, OCT_15));
        assertThrows(BadRequestException.class, () -> service.run(OCT_1, null));
        assertThrows(BadRequestException.class, () -> service.run(OCT_15, OCT_1));
        assertThrows(BadRequestException.class, () -> service.run(OCT_15.minusDays(92), OCT_15), "93 days");
        service.run(OCT_15.minusDays(91), OCT_15); // 92 days is the most
    }

    @Test
    void runningTheChecksChangesNothing() {
        startPlan("2026-06-01");
        entry("2026-10-02 09:00", "deviation");
        long before = jdbc.queryForObject("SELECT (SELECT count(*) FROM invest_journal_entries) + (SELECT count(*) FROM invest_settings_changes) "
                + "+ (SELECT count(*) FROM invest_tags) + (SELECT count(*) FROM invest_contributions)", Long.class);
        service.run(OCT_1, OCT_15);
        long after = jdbc.queryForObject("SELECT (SELECT count(*) FROM invest_journal_entries) + (SELECT count(*) FROM invest_settings_changes) "
                + "+ (SELECT count(*) FROM invest_tags) + (SELECT count(*) FROM invest_contributions)", Long.class);
        assertEquals(before, after);
    }
}
