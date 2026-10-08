package trademind.tm_server.invest.checkin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
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
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.rulecheck.RuleCheckService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The month's facts computed from real records. Fixed clock 2026-10-15 UTC; the plan starts 2026-08-01, buy day 1. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CheckinFactsServiceTest.TestApp.class)
@Import({CheckinFactsService.class, ContributionService.class, RuleCheckService.class, CheckinFactsServiceTest.FixedClock.class})
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
                + "classpath:db/migration/V29__invest_settings_changes.sql"
})

class CheckinFactsServiceTest {

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

    @Autowired CheckinFactsService service;
    @Autowired InvestSettingsRepository settings;
    @Autowired JdbcTemplate jdbc;

    private static final YearMonth SEP = YearMonth.of(2026, 9);

    @BeforeEach
    void startPlanInAugust() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
    }

    private long buy(String date, long cents, String shares, String price, long fee) {
        return jdbc.queryForObject("INSERT INTO invest_contributions (contributed_on, amount_cents, shares, price, fee_cents) "
                + "VALUES (?::date, ?, ?::numeric, ?::numeric, ?) RETURNING id", Long.class, date, cents, shares, price, fee);
    }

    private void close(String date, String close) {
        jdbc.update("INSERT INTO invest_prices (ticker, price_date, close) VALUES ('VT', ?::date, ?::numeric)", date, close);
    }

    private long entry(String createdAt, Integer mood, String... tags) {
        long id = jdbc.queryForObject("INSERT INTO invest_journal_entries (entry_type, mood, body, created_at) "
                + "VALUES ('QUICK', ?, 'x', ?::timestamp) RETURNING id", Long.class, mood, createdAt);
        for (String tag : tags) {
            Long tagId = jdbc.query("SELECT id FROM invest_tags WHERE name = ?", rs -> rs.next() ? rs.getLong(1) : null, tag);
            if (tagId == null) tagId = jdbc.queryForObject("INSERT INTO invest_tags (name) VALUES (?) RETURNING id", Long.class, tag);
            jdbc.update("INSERT INTO invest_journal_entry_tags (entry_id, tag_id) VALUES (?, ?)", id, tagId);
        }
        return id;
    }

    @Test
    void theMonthsBuyIsDescribedWithItsTimingAverageAndLink() {
        buy("2026-08-01", 30000, "3", "90.0000", 0);
        long sep = buy("2026-09-05", 30000, "3", "100.0000", 100);
        CheckinFacts f = service.load(SEP).facts();
        assertEquals("2026-09", f.month());
        assertEquals("USD", f.currency());
        assertEquals(30000, f.plannedCents());
        assertEquals(1, f.buyDay());
        assertEquals(LocalDate.parse("2026-09-01"), f.dueDate());
        assertEquals("DONE", f.state());
        assertEquals(1, f.buys().size());
        assertEquals(30000, f.investedCents());
        assertEquals(100, f.feesCents());
        assertEquals(0, new BigDecimal("3").compareTo(f.sharesBought()));
        assertEquals(new BigDecimal("100.0000"), f.averagePrice());
        assertEquals(LocalDate.parse("2026-09-05"), f.firstBuyDate());
        assertEquals(4, f.daysAfterDue());
        assertEquals(sep, f.contributionId());
    }

    @Test
    void earlyAndOnTheDayBuysHaveNegativeAndZeroOffsets() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setBuyDay((short) 10);
        settings.saveAndFlush(s);
        buy("2026-09-08", 30000, "3", "100.0000", 0);
        assertEquals(-2, service.load(SEP).facts().daysAfterDue());
        jdbc.update("DELETE FROM invest_contributions");
        buy("2026-09-10", 30000, "3", "100.0000", 0);
        assertEquals(0, service.load(SEP).facts().daysAfterDue());
    }

    @Test
    void severalBuysInAMonthAreSummedAndTheLatestIsTheLink() {
        buy("2026-09-02", 10000, "1", "100.0000", 0);
        long later = buy("2026-09-20", 20000, "2", "100.0000", 0);
        CheckinFacts f = service.load(SEP).facts();
        assertEquals(30000, f.investedCents());
        assertEquals(2, f.buys().size());
        assertEquals(LocalDate.parse("2026-09-02"), f.firstBuyDate());
        assertEquals(later, f.contributionId());
    }

    @Test
    void monthEndValueUsesTheLastCloseOnOrBeforeTheEndAndEverythingBoughtSoFar() {
        buy("2026-08-01", 30000, "3", "90.0000", 0);
        buy("2026-09-05", 30000, "3", "100.0000", 100);
        close("2026-09-29", "105.0000");
        close("2026-09-30", "110.0000");
        close("2026-10-02", "999.0000"); // after the month: must not be used
        CheckinFacts.MonthEndValue v = service.load(SEP).facts().monthEnd();
        assertNotNull(v);
        assertEquals(LocalDate.parse("2026-09-30"), v.asOf());
        assertEquals(60100, v.investedToDateCents(), "both buys, fee included");
        assertEquals(66000, v.valueCents(), "6 shares at 110");
        assertEquals(5900, v.gainCents());
        assertEquals(new BigDecimal("9.82"), v.gainPercent());
    }

    @Test
    void withoutACachedPriceThereIsNoMonthEndValue() {
        buy("2026-09-05", 30000, "3", "100.0000", 0);
        assertNull(service.load(SEP).facts().monthEnd());
        close("2026-10-02", "120.0000"); // only after the month
        assertNull(service.load(SEP).facts().monthEnd());
    }

    @Test
    void aBuyAfterTheMonthEndIsNotInThatMonthsValue() {
        buy("2026-09-05", 30000, "3", "100.0000", 0);
        buy("2026-10-03", 30000, "3", "100.0000", 0);
        close("2026-09-30", "110.0000");
        assertEquals(33000, service.load(SEP).facts().monthEnd().valueCents());
    }

    @Test
    void streakAndMissesComeFromTheRealStatus() {
        buy("2026-08-01", 30000, "3", "90.0000", 0);
        buy("2026-09-05", 30000, "3", "100.0000", 0);
        CheckinFacts f = service.load(SEP).facts();
        assertEquals(2, f.currentStreak());
        assertEquals(2, f.longestStreak());
        assertEquals(0, f.missedMonths());
    }

    @Test
    void aMonthWithoutABuyIsMissedAndHasNoTiming() {
        buy("2026-08-01", 30000, "3", "90.0000", 0);
        CheckinFacts f = service.load(SEP).facts();
        assertEquals("MISSED", f.state());
        assertEquals(List.of(), f.buys());
        assertEquals(0, f.investedCents());
        assertNull(f.averagePrice());
        assertNull(f.firstBuyDate());
        assertNull(f.daysAfterDue());
        assertNull(f.contributionId());
        assertEquals(1, f.missedMonths());
    }

    @Test
    void theCurrentMonthIsSoFarAndOverdueUntilABuy() {
        buy("2026-08-01", 30000, "3", "90.0000", 0);
        buy("2026-09-01", 30000, "3", "100.0000", 0);
        close("2026-10-14", "120.0000");
        close("2026-10-20", "999.0000"); // in the future of the fixed clock
        CheckinFacts f = service.load(YearMonth.of(2026, 10)).facts();
        assertEquals("OVERDUE", f.state());
        assertEquals(0, f.investedCents());
        assertEquals(LocalDate.parse("2026-10-14"), f.monthEnd().asOf());
        assertEquals(72000, f.monthEnd().valueCents(), "6 shares at 120");
    }

    @Test
    void moodsAverageAndTagCountsCoverEveryLiveEntryOfTheMonthOnly() {
        entry("2026-08-31 23:59:00", 5, "calm");        // the day before
        entry("2026-09-03 09:00:00", 2, "fear");
        entry("2026-09-20 09:00:00", 4, "fear", "decision");
        entry("2026-09-21 09:00:00", null);
        long gone = entry("2026-09-22 09:00:00", 1, "fear", "panic");
        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now() WHERE id = ?", gone);
        entry("2026-10-01 00:00:00", 5, "calm");        // the day after
        CheckinFacts f = service.load(SEP).facts();
        assertEquals(3, f.entryCount());
        assertEquals(List.of(2, 4), f.moods());
        assertEquals(new BigDecimal("3.0"), f.moodAverage());
        assertEquals(List.of(new CheckinFacts.TagCount("fear", 2), new CheckinFacts.TagCount("decision", 1)), f.tags());
    }

    @Test
    void aDeletedTagIsNotCounted() {
        entry("2026-09-03 09:00:00", 2, "fear", "regret");
        jdbc.update("UPDATE invest_tags SET deleted_at = now() WHERE name = 'regret'");
        CheckinFacts f = service.load(SEP).facts();
        assertEquals(List.of(new CheckinFacts.TagCount("fear", 1)), f.tags());
    }

    @Test
    void noEntriesMeansNoMoodAverage() {
        CheckinFacts f = service.load(SEP).facts();
        assertEquals(0, f.entryCount());
        assertEquals(List.of(), f.moods());
        assertNull(f.moodAverage());
        assertEquals(List.of(), f.tags());
    }

    @Test
    void theMonthsEntriesAreReturnedOldestFirstForTheSummary() {
        long a = entry("2026-09-03 09:00:00", 2, "fear");
        long b = entry("2026-09-20 09:00:00", null);
        List<CheckinFactsService.MonthEntry> entries = service.load(SEP).entries();
        assertEquals(List.of(a, b), entries.stream().map(CheckinFactsService.MonthEntry::id).toList());
        assertEquals(List.of("fear"), entries.get(0).tags());
        assertEquals((short) 2, entries.get(0).mood());
        assertNull(entries.get(1).mood());
    }

    @Test
    void ruleNotesAreTheExactChecksForThatMonth() {
        buy("2026-08-01", 30000, "3", "90.0000", 0); // August was not missed
        entry("2026-09-10 09:00:00", null, "deviation"); // no decision entry before it
        List<String> notes = service.load(SEP).facts().ruleNotes();
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("records a deviation"), notes.get(0));
        assertEquals(List.of(), service.load(YearMonth.of(2026, 8)).facts().ruleNotes());
    }

    @Test
    void theMonthIsValidated() {
        for (String bad : new String[] {null, "", "2026-9", "2026-13", "2026-00", "september", "2026-09-01", "26-09"}) {
            assertEquals("Choose a month like 2026-09", assertThrows(BadRequestException.class, () -> service.parse(bad)).getMessage(), bad);
        }
        assertEquals("That month is before your plan started", assertThrows(BadRequestException.class, () -> service.parse("2026-07")).getMessage());
        assertEquals("That month has not started yet", assertThrows(BadRequestException.class, () -> service.parse("2026-11")).getMessage());
        assertEquals(YearMonth.of(2026, 8), service.parse("2026-08"), "the start month itself is fine");
        assertEquals(YearMonth.of(2026, 10), service.parse("2026-10"), "so is the current month");
    }

    @Test
    void loadingChangesNothing() {
        buy("2026-09-05", 30000, "3", "100.0000", 0);
        entry("2026-09-03 09:00:00", 2, "fear");
        String counts = "SELECT (SELECT count(*) FROM invest_contributions) + (SELECT count(*) FROM invest_journal_entries) "
                + "+ (SELECT count(*) FROM invest_tags) + (SELECT count(*) FROM invest_prices) + (SELECT count(*) FROM invest_settings_changes)";
        long before = jdbc.queryForObject(counts, Long.class);
        service.load(SEP);
        assertEquals(before, jdbc.queryForObject(counts, Long.class));
    }
}
