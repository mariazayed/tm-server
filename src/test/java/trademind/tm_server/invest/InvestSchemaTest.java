package trademind.tm_server.invest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.enums.SourceType;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.InvestTagRepository;
import trademind.tm_server.invest.repository.JournalEntryRepository;
import trademind.tm_server.invest.repository.LearningNoteRepository;
import trademind.tm_server.invest.repository.LearningSourceRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;
import trademind.tm_server.invest.repository.RuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the real V22 migration to a throwaway PostgreSQL schema and lets Hibernate validate the
 * entities against it. Runs only when INVEST_TEST_DB_URL (plus _USER/_PASSWORD) is set, e.g. in CI
 * with a PostgreSQL service. WARNING: it drops and recreates the invest_* tables in that database.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = InvestSchemaTest.TestApp.class)
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
class InvestSchemaTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    @Autowired InvestSettingsRepository settings;
    @Autowired ContributionRepository contributions;
    @Autowired trademind.tm_server.invest.repository.HoldingRepository holdings;
    @Autowired PriceQuoteRepository prices;
    @Autowired InvestTagRepository tags;
    @Autowired JournalEntryRepository journal;
    @Autowired RuleRepository rules;
    @Autowired LearningSourceRepository sources;
    @Autowired LearningNoteRepository notes;

    @Test
    void migrationSeedsSettingsTagsAndRules() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        assertEquals(30000, s.getMonthlyAmountCents());
        assertEquals("VT", s.getTicker());
        assertEquals(4, tags.findByDeletedAtIsNullOrderByNameAsc().size());
        assertEquals(3, rules.findByDeletedAtIsNullOrderBySortOrderAsc().size());
    }

    @Test
    void contributionKeepsExactDecimalsAndSoftDeletes() {
        Contribution c = new Contribution();
        c.setHolding(holdings.findAll().get(0));
        c.setContributedOn(LocalDate.of(2026, 10, 1));
        c.setAmountCents(30000);
        c.setShares(new BigDecimal("2.55489022"));
        c.setPrice(new BigDecimal("117.4250"));
        contributions.saveAndFlush(c);

        Contribution loaded = contributions.findByIdAndDeletedAtIsNull(c.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("2.55489022").compareTo(loaded.getShares()));
        assertEquals(0, new BigDecimal("117.4250").compareTo(loaded.getPrice()));

        loaded.setDeletedAt(LocalDateTime.now());
        contributions.saveAndFlush(loaded);
        assertTrue(contributions.findByIdAndDeletedAtIsNull(c.getId()).isEmpty());
    }

    @Test
    void constraintsRejectBadData() {
        Contribution zero = new Contribution();
        zero.setHolding(holdings.findAll().get(0));
        zero.setContributedOn(LocalDate.of(2026, 10, 1));
        zero.setAmountCents(0);
        zero.setShares(BigDecimal.ONE);
        zero.setPrice(BigDecimal.ONE);
        assertThrows(DataIntegrityViolationException.class, () -> contributions.saveAndFlush(zero));
    }

    // A constraint violation aborts the surrounding PostgreSQL transaction, so each of these
    // tests ends with its failing statement.
    @Test
    void priceIsUniquePerTickerAndDate() {
        prices.saveAndFlush(new PriceQuote("VT", LocalDate.of(2026, 10, 1), new BigDecimal("117.42")));
        assertEquals(0, new BigDecimal("117.4200").compareTo(
                prices.findFirstByTickerOrderByPriceDateDesc("VT").orElseThrow().getClose()));
        assertThrows(DataIntegrityViolationException.class, () -> prices.saveAndFlush(
                new PriceQuote("VT", LocalDate.of(2026, 10, 1), new BigDecimal("118.00"))));
    }

    @Test
    void tagNamesAreUniqueIgnoringCaseAndReusableAfterDelete() {
        InvestTag emotion = tags.findByNameIgnoreCaseAndDeletedAtIsNull("emotion").orElseThrow();
        emotion.setDeletedAt(LocalDateTime.now());
        tags.saveAndFlush(emotion);
        tags.saveAndFlush(new InvestTag("Emotion")); // name is free again once deleted

        assertThrows(DataIntegrityViolationException.class,
                () -> tags.saveAndFlush(new InvestTag("EMOTION")));
    }

    @Test
    void journalEntryLinksTagsAndContribution() {
        Contribution c = new Contribution();
        c.setHolding(holdings.findAll().get(0));
        c.setContributedOn(LocalDate.of(2026, 10, 1));
        c.setAmountCents(30000);
        c.setShares(BigDecimal.ONE);
        c.setPrice(new BigDecimal("300"));
        contributions.saveAndFlush(c);

        JournalEntry e = new JournalEntry();
        e.setEntryType(JournalEntryType.CHECKIN);
        e.setMood((short) 4);
        e.setBody("Stuck to the plan.");
        e.setContribution(c);
        e.getTags().add(tags.findByNameIgnoreCaseAndDeletedAtIsNull("learning").orElseThrow());
        journal.saveAndFlush(e);

        Long tagId = tags.findByNameIgnoreCaseAndDeletedAtIsNull("learning").orElseThrow().getId();
        assertEquals(1, journal.findByDeletedAtIsNullAndTags_IdOrderByCreatedAtDesc(tagId).size());

        JournalEntry badMood = new JournalEntry();
        badMood.setEntryType(JournalEntryType.QUICK);
        badMood.setMood((short) 9);
        badMood.setBody("x");
        assertThrows(DataIntegrityViolationException.class, () -> journal.saveAndFlush(badMood));
    }

    @Test
    void learningNoteLinksSourceAndTags() {
        LearningSource src = new LearningSource();
        src.setTitle("The Simple Path to Wealth");
        src.setSourceType(SourceType.BOOK);
        sources.saveAndFlush(src);
        assertFalse(sources.findByDeletedAtIsNullOrderByCreatedAtDesc().isEmpty());

        LearningNote n = new LearningNote();
        n.setTitle("DCA");
        n.setBody("Invest on a schedule.");
        n.setSource(src);
        n.getTags().add(tags.findByNameIgnoreCaseAndDeletedAtIsNull("learning").orElseThrow());
        notes.saveAndFlush(n);
        assertEquals(1, notes.findByDeletedAtIsNullAndSource_IdOrderByCreatedAtDesc(src.getId()).size());
    }

    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void flashcardEaseCannotDropBelowTheSm2Floor() {
        Flashcard ok = new Flashcard();
        ok.setFront("Q");
        ok.setBack("A");
        ok.setDueOn(LocalDate.of(2026, 10, 15));
        ok.setEase(new BigDecimal("1.30"));
        entityManager.persist(ok);
        entityManager.flush();

        Flashcard bad = new Flashcard();
        bad.setFront("Q");
        bad.setBack("A");
        bad.setDueOn(LocalDate.of(2026, 10, 15));
        bad.setEase(new BigDecimal("1.29"));
        assertThrows(Exception.class, () -> {
            entityManager.persist(bad);
            entityManager.flush();
        });
    }
}
