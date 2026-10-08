package trademind.tm_server.invest.journal;

import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.journal.JournalDtos.CheckinTemplate;
import trademind.tm_server.invest.journal.JournalDtos.EntryRequest;
import trademind.tm_server.invest.journal.JournalDtos.EntryResponse;
import trademind.tm_server.invest.journal.JournalDtos.PageResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs a scratch PostgreSQL; see InvestSchemaTest. Fixed clock: 2026-10-15 UTC. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = JournalServiceTest.TestApp.class)
@Import({JournalService.class, trademind.tm_server.invest.TagService.class, ContributionService.class, JournalServiceTest.FixedClock.class})
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
                + "classpath:db/migration/V27__invest_fulltext_search.sql"
})
class JournalServiceTest {

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

    @Autowired JournalService journal;
    @Autowired ContributionService contributions;
    @Autowired InvestSettingsRepository settings;
    @Autowired EntityManager em;

    @BeforeEach
    void planStartsInAugust() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
    }

    private static EntryRequest entry(JournalEntryType type, String body, String... tags) {
        return new EntryRequest(type, null, body, List.of(tags), null);
    }

    private static JournalService.Filter filter(String tag, JournalEntryType type, String q) {
        return new JournalService.Filter(tag, type, q, null, null);
    }

    private void backdate(Long id, String timestamp) {
        em.createNativeQuery("update invest_journal_entries set created_at = cast(:t as timestamp) where id = :id")
                .setParameter("t", timestamp).setParameter("id", id).executeUpdate();
        em.clear();
    }

    @Test
    void createReadUpdateDeleteLifecycle() {
        EntryResponse created = journal.create(new EntryRequest(JournalEntryType.QUICK, (short) 2,
                "  Headlines are loud.  ", List.of("Emotion", "#market event"), null));
        assertEquals("Headlines are loud.", created.body());
        assertEquals(List.of("emotion", "market event"), created.tags());
        assertEquals((short) 2, created.mood());

        EntryResponse updated = journal.update(created.id(),
                new EntryRequest(JournalEntryType.DECISION, null, "Waiting 72 hours.", List.of("decision"), null));
        assertEquals(JournalEntryType.DECISION, updated.entryType());
        assertEquals(List.of("decision"), updated.tags());
        assertNull(updated.mood());

        journal.delete(created.id());
        assertThrows(ResourceNotFoundException.class, () -> journal.get(created.id()));
        assertEquals(0, journal.list(filter(null, null, null), 0, 10).total());
    }

    @Test
    void tagsAreCreatedOnFirstUseAndReusedCaseInsensitively() {
        journal.create(entry(JournalEntryType.QUICK, "one", "Brand-New"));
        journal.create(entry(JournalEntryType.QUICK, "two", "brand-new", "emotion"));

        var usage = journal.tags();
        var brandNew = usage.stream().filter(t -> t.name().equals("brand-new")).findFirst().orElseThrow();
        assertEquals(2, brandNew.count());
        assertEquals(1, usage.stream().filter(t -> t.name().equals("brand-new")).count()); // no duplicate tag row
        assertEquals(1, usage.stream().filter(t -> t.name().equals("emotion")).findFirst().orElseThrow().count());
        assertEquals(0, usage.stream().filter(t -> t.name().equals("learning")).findFirst().orElseThrow().count());
    }

    @Test
    void timelineFiltersByTagTypeAndText() {
        journal.create(entry(JournalEntryType.QUICK, "Market dropped 3% today", "emotion"));
        journal.create(entry(JournalEntryType.DECISION, "Wait before changing anything", "decision", "emotion"));
        journal.create(entry(JournalEntryType.CHECKIN, "All good, 100% on plan"));

        assertEquals(2, journal.list(filter("emotion", null, null), 0, 10).total());
        assertEquals(2, journal.list(filter("#Emotion", null, null), 0, 10).total()); // # and case ignored
        assertEquals(1, journal.list(filter("emotion", JournalEntryType.DECISION, null), 0, 10).total());
        assertEquals(1, journal.list(filter(null, null, "DROPPED"), 0, 10).total()); // case-insensitive
        assertEquals(0, journal.list(filter("nonexistent", null, null), 0, 10).total());
    }

    @Test
    void searchTreatsPercentAndUnderscoreLiterally() {
        journal.create(entry(JournalEntryType.QUICK, "Dropped 3% today"));
        journal.create(entry(JournalEntryType.QUICK, "Plain words"));
        journal.create(entry(JournalEntryType.QUICK, "snake_case words"));

        assertEquals(1, journal.list(filter(null, null, "%"), 0, 10).total()); // not "match everything"
        assertEquals(1, journal.list(filter(null, null, "_"), 0, 10).total());
        assertEquals(0, journal.list(filter(null, null, "3_ today"), 0, 10).total());
    }

    @Test
    void timelineFiltersByDateRangeIncludingTheLastDay() {
        EntryResponse oct1 = journal.create(entry(JournalEntryType.QUICK, "early"));
        EntryResponse oct31 = journal.create(entry(JournalEntryType.QUICK, "late"));
        EntryResponse nov1 = journal.create(entry(JournalEntryType.QUICK, "next month"));
        backdate(oct1.id(), "2026-10-01 00:00:00");
        backdate(oct31.id(), "2026-10-31 23:59:59");
        backdate(nov1.id(), "2026-11-01 00:00:00");

        var oct = new JournalService.Filter(null, null, null, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
        PageResponse page = journal.list(oct, 0, 10);
        assertEquals(2, page.total());
        assertEquals(List.of("late", "early"), page.items().stream().map(EntryResponse::body).toList());

        assertThrows(BadRequestException.class, () -> journal.list(
                new JournalService.Filter(null, null, null, LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01")), 0, 10));
    }

    @Test
    void pagesNewestFirstAndReportsHasMore() {
        for (int i = 1; i <= 5; i++) {
            EntryResponse e = journal.create(entry(JournalEntryType.QUICK, "note " + i, "emotion"));
            backdate(e.id(), "2026-10-0" + i + " 09:00:00");
        }
        PageResponse first = journal.list(filter(null, null, null), 0, 2);
        assertEquals(List.of("note 5", "note 4"), first.items().stream().map(EntryResponse::body).toList());
        assertTrue(first.hasMore());
        assertEquals(5, first.total());
        assertEquals(List.of("emotion"), first.items().get(0).tags()); // tags hydrated on paged items

        PageResponse last = journal.list(filter(null, null, null), 2, 2);
        assertEquals(List.of("note 3", "note 2"), journal.list(filter(null, null, null), 1, 2).items().stream()
                .map(EntryResponse::body).toList());
        assertEquals(List.of("note 1"), last.items().stream().map(EntryResponse::body).toList());
        assertEquals(false, last.hasMore());

        assertThrows(BadRequestException.class, () -> journal.list(filter(null, null, null), 0, 1000));
        assertThrows(BadRequestException.class, () -> journal.list(filter(null, null, null), -1, 10));
    }

    @Test
    void entriesCanLinkToALiveContributionOnly() {
        Long buyId = contributions.create(new ContributionRequest(LocalDate.parse("2026-10-01"), 30000L,
                new BigDecimal("2.5549"), new BigDecimal("117.425"), null, null)).id();

        EntryResponse linked = journal.create(new EntryRequest(JournalEntryType.CHECKIN, (short) 4, "Bought.", List.of(), buyId));
        assertEquals(buyId, linked.contributionId());

        assertThrows(BadRequestException.class,
                () -> journal.create(new EntryRequest(JournalEntryType.QUICK, null, "x", List.of(), 99999L)));

        contributions.delete(buyId);
        assertThrows(BadRequestException.class,
                () -> journal.create(new EntryRequest(JournalEntryType.QUICK, null, "x", List.of(), buyId)));
    }

    @Test
    void anEntryForgetsItsLinkWhenTheBuyIsDeleted() {
        Long buyId = contributions.create(new ContributionRequest(LocalDate.parse("2026-10-01"), 30000L,
                new BigDecimal("2.5549"), new BigDecimal("117.425"), null, null)).id();
        EntryResponse linked = journal.create(new EntryRequest(JournalEntryType.CHECKIN, null, "Bought.", List.of(), buyId));
        assertEquals(buyId, journal.get(linked.id()).contributionId());

        contributions.delete(buyId);
        em.flush();
        em.clear();
        assertNull(journal.get(linked.id()).contributionId());
        assertNull(journal.list(filter(null, null, null), 0, 10).items().get(0).contributionId());
    }

    @Test
    void checkinTemplateReflectsThisMonthsContribution() {
        CheckinTemplate before = journal.checkinTemplate();
        assertEquals("2026-10", before.month());
        assertNull(before.contributionId());
        assertTrue(before.body().contains("## Month check-in: October 2026"));
        assertTrue(before.body().contains("**Contribution:** overdue, nothing bought yet this month")); // buy day 1 has passed

        Long id = contributions.create(new ContributionRequest(LocalDate.parse("2026-10-02"), 30000L,
                new BigDecimal("2.5549"), new BigDecimal("117.425"), null, null)).id();
        CheckinTemplate after = journal.checkinTemplate();
        assertEquals(id, after.contributionId());
        assertTrue(after.body().contains("**Contribution:** done (1 buy, $300.00)"));
    }
}
