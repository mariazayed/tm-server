package trademind.tm_server.invest.holding;

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
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Needs a scratch PostgreSQL; see InvestSchemaTest. After the migration there is one fund, made from the
 * settings ticker (VT). Several funds are switched on for this class.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = HoldingsOffByDefaultTest.TestApp.class)
@Import({HoldingService.class, ContributionService.class, SettingsService.class, HoldingsOffByDefaultTest.FixedClock.class})
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.datasource.hikari.maximum-pool-size=2",
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
class HoldingsOffByDefaultTest {

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

    @Autowired HoldingService holdings;

    private static HoldingRequest fund(String ticker) {
        return new HoldingRequest(ticker, null, Holding.AssetClass.BONDS, "United States",
                new BigDecimal("25"), null, null);
    }

    @Test
    void aSecondFundIsRefusedUntilSeveralFundsAreSwitchedOn() {
        BadRequestException e = assertThrows(BadRequestException.class, () -> holdings.create(fund("AGG")));
        assertTrue(e.getMessage().contains("not switched on"), e.getMessage());
        assertEquals(1, holdings.list().size());
    }

    @Test
    void theOnlyFundCanStillBeEdited() {
        Long id = holdings.list().get(0).id();
        assertEquals("VOO", holdings.update(id, fund("VOO")).ticker());
    }
}
