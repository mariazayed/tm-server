package trademind.tm_server.invest.contribution;

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
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.dto.ImportResult;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs a scratch PostgreSQL; see InvestSchemaTest. Fixed clock: 2026-10-15 UTC. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ContributionServiceTest.TestApp.class)
@Import({ContributionService.class, ContributionServiceTest.FixedClock.class})
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:invest-schema-reset.sql,"
                + "classpath:db/migration/V22__create_invest_tables.sql,"
                + "classpath:db/migration/V23__create_invest_flashcards.sql,"
                + "classpath:db/migration/V24__invest_reminders.sql"
})
class ContributionServiceTest {

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

    @Autowired ContributionService service;
    @Autowired InvestSettingsRepository settings;

    @BeforeEach
    void startPlanInAugust() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
    }

    private static ContributionRequest req(String date, long cents, String shares, String price) {
        return new ContributionRequest(LocalDate.parse(date), cents, new BigDecimal(shares),
                new BigDecimal(price), null, null);
    }

    @Test
    void createUpdateDeleteLifecycle() {
        ContributionResponse created = service.create(req("2026-10-01", 30000, "2.5549", "117.425"));
        assertEquals(0L, created.feeCents());

        ContributionResponse updated = service.update(created.id(), req("2026-10-02", 30000, "2.5549", "117.425"));
        assertEquals(LocalDate.parse("2026-10-02"), updated.contributedOn());
        assertEquals(1, service.list().size());

        service.delete(created.id());
        assertTrue(service.list().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(created.id()));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(created.id()));
    }

    @Test
    void createRejectsFutureDateAndInconsistentShares() {
        assertThrows(BadRequestException.class, () -> service.create(req("2026-10-16", 30000, "2.5549", "117.425")));
        assertThrows(BadRequestException.class, () -> service.create(req("2026-10-01", 30000, "9", "117.425")));
        assertTrue(service.list().isEmpty());
    }

    @Test
    void statusReflectsMissedMonthsStreakAndOpenCurrentMonth() {
        service.create(req("2026-08-01", 30000, "2.5549", "117.425"));
        // September missed; October's buy day (1st) has passed without a buy.
        ContributionStatusCalculator.Result r = service.status();
        assertEquals(3, r.months().size());
        assertEquals(1, r.missedCount());
        assertEquals(ContributionStatusCalculator.MonthState.OVERDUE, r.months().get(2).state());
        assertEquals(0, r.currentStreak());

        service.create(req("2026-10-01", 30000, "2.5549", "117.425"));
        assertEquals(1, service.status().currentStreak());
    }

    @Test
    void importIsAllOrNothingAndRepeatableWithoutDuplicates() {
        String good = "date,amount,shares,price\n2026-08-01,300,2.5549,117.425\n2026-09-01,300,2.5549,117.425\n";
        ImportResult first = service.importCsv(good);
        assertEquals(2, first.imported());
        assertEquals(0, first.skipped());

        ImportResult again = service.importCsv(good);
        assertEquals(0, again.imported());
        assertEquals(2, again.skipped());
        assertEquals(2, service.list().size());

        String mixed = "date,amount,shares,price\n2026-10-01,300,2.5549,117.425\n2026-10-02,300,99,117.425\n";
        ImportResult rejected = service.importCsv(mixed);
        assertEquals(0, rejected.imported());
        assertEquals(1, rejected.errors().size());
        assertEquals(3L, rejected.errors().get(0).line());
        assertEquals(2, service.list().size()); // the valid row was not saved either
    }

    @Test
    void exportReimportsCleanly() {
        service.create(req("2026-09-01", 30000, "2.5549", "117.425"));
        String csv = service.exportCsv();
        ImportResult result = service.importCsv(csv);
        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().isEmpty());
    }
}
