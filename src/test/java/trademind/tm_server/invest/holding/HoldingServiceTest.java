package trademind.tm_server.invest.holding;

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
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.dto.ImportResult;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.SettingsChangeRepository;
import trademind.tm_server.invest.settings.SettingsRequest;
import trademind.tm_server.invest.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Needs a scratch PostgreSQL; see InvestSchemaTest. After the migration there is one fund, made from the
 * settings ticker (VT). Several funds are switched on for this class.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = HoldingServiceTest.TestApp.class)
@Import({HoldingService.class, ContributionService.class, SettingsService.class, HoldingServiceTest.FixedClock.class})
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "invest.holdings.multiple=true",
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
class HoldingServiceTest {

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
    @Autowired ContributionService contributions;
    @Autowired SettingsService settingsService;
    @Autowired InvestSettingsRepository settings;
    @Autowired SettingsChangeRepository changes;

    @BeforeEach
    void startPlanInAugust() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        s.setBuyDay((short) 1);
        settings.saveAndFlush(s);
    }

    private static HoldingRequest fund(String ticker, String pct) {
        return new HoldingRequest(ticker, ticker + " fund", Holding.AssetClass.BONDS, "United States",
                new BigDecimal(pct), new BigDecimal("0.050"), null);
    }

    private static ContributionRequest buy(String date, String ticker) {
        return new ContributionRequest(LocalDate.parse(date), 30000L, new BigDecimal("3"), new BigDecimal("100"),
                null, null, null, ticker);
    }

    private static ContributionRequest buyWithoutFund(String date) {
        return new ContributionRequest(LocalDate.parse(date), 30000L, new BigDecimal("3"), new BigDecimal("100"),
                null, null);
    }

    @Test
    void theMigrationMadeOneFundFromTheSettingsTicker() {
        List<HoldingResponse> list = holdings.list();
        assertEquals(1, list.size());
        assertEquals("VT", list.get(0).ticker());
        assertEquals(Holding.AssetClass.EQUITY, list.get(0).assetClass());
        assertEquals(0, new BigDecimal("100").compareTo(list.get(0).targetPct()));
    }

    @Test
    void createsAFundAndNormalisesTheTicker() {
        HoldingResponse created = holdings.create(fund("agg", "25"));
        assertEquals("AGG", created.ticker());
        assertEquals(2, holdings.list().size());
        assertEquals("VT", holdings.list().get(0).ticker()); // the first one stays first
    }

    @Test
    void aTickerCanOnlyBeUsedOnce_whateverTheCase() {
        holdings.create(fund("AGG", "25"));
        assertThrows(BadRequestException.class, () -> holdings.create(fund("agg", "10")));
        assertThrows(BadRequestException.class, () -> holdings.create(fund("vt", "10")));
        Long agg = holdings.list().get(1).id();
        // editing a fund may keep its own ticker
        assertEquals("AGG", holdings.update(agg, fund("AGG", "30")).ticker());
        assertThrows(BadRequestException.class, () -> holdings.update(agg, fund("VT", "30")));
    }

    @Test
    void settingsTickerFollowsTheFirstFund_andTheChangeIsLogged() {
        long before = changes.count();
        Long vt = holdings.list().get(0).id();
        holdings.update(vt, fund("VTI", "100"));
        assertEquals("VTI", settings.findById(InvestSettings.SINGLETON_ID).orElseThrow().getTicker());
        assertEquals(before + 1, changes.count());
        var all = changes.findByDeletedAtIsNullOrderByCreatedAtAscIdAsc();
        var last = all.get(all.size() - 1);
        assertEquals("TICKER", last.getFieldName());
        assertEquals("VT", last.getOldValue());
        assertEquals("VTI", last.getNewValue());

        holdings.update(vt, fund("VTI", "60")); // other edits are not plan changes
        assertEquals(before + 1, changes.count());
    }

    @Test
    void aFundWithBuysCannotBeDeleted_andTheLastFundNeverCan() {
        holdings.create(fund("AGG", "25"));
        contributions.create(buy("2026-10-01", "AGG"));
        Long agg = holdings.list().get(1).id();
        Long vt = holdings.list().get(0).id();
        assertThrows(BadRequestException.class, () -> holdings.delete(agg)); // has a buy
        holdings.delete(vt); // no buys: fine, AGG remains
        assertEquals(List.of("AGG"), holdings.list().stream().map(HoldingResponse::ticker).toList());
        assertEquals("AGG", settings.findById(InvestSettings.SINGLETON_ID).orElseThrow().getTicker());
        assertThrows(BadRequestException.class, () -> holdings.delete(agg)); // the last one
    }

    @Test
    void oldClientsChangingTheTickerRenameTheOnlyFund() {
        settingsService.update(new SettingsRequest(30000L, "USD", 1, "vwrl", LocalDate.parse("2026-08-01"), "UTC",
                false, null, 3));
        assertEquals(List.of("VWRL"), holdings.list().stream().map(HoldingResponse::ticker).toList());
    }

    @Test
    void withSeveralFundsTheLegacyTickerIsIgnored() {
        holdings.create(fund("AGG", "25"));
        settingsService.update(new SettingsRequest(30000L, "USD", 1, "ZZZ", LocalDate.parse("2026-08-01"), "UTC",
                false, null, 3));
        assertEquals(List.of("VT", "AGG"), holdings.list().stream().map(HoldingResponse::ticker).toList());
    }

    @Test
    void aSettingsRequestWithoutATickerKeepsTheFunds() {
        settingsService.update(new SettingsRequest(45000L, "USD", 2, null, LocalDate.parse("2026-08-01"), "UTC",
                false, null, 3));
        assertEquals(List.of("VT"), holdings.list().stream().map(HoldingResponse::ticker).toList());
        assertEquals(45000L, settings.findById(InvestSettings.SINGLETON_ID).orElseThrow().getMonthlyAmountCents());
    }

    // ----------------------------------------------------- buys and their fund

    @Test
    void aBuyWithNoFundUsesTheOnlyFund_butIsRefusedWhenThereAreSeveral() {
        ContributionResponse only = contributions.create(buyWithoutFund("2026-10-01"));
        assertEquals("VT", only.ticker());

        holdings.create(fund("AGG", "25"));
        BadRequestException e = assertThrows(BadRequestException.class, () -> contributions.create(buyWithoutFund("2026-10-02")));
        assertTrue(e.getMessage().contains("Choose which fund"), e.getMessage());
    }

    @Test
    void aBuyCanNameItsFundByTickerOrId() {
        holdings.create(fund("AGG", "25"));
        Long aggId = holdings.list().get(1).id();
        assertEquals("AGG", contributions.create(buy("2026-10-01", "agg")).ticker());
        ContributionResponse byId = contributions.create(new ContributionRequest(LocalDate.parse("2026-10-02"), 30000L,
                new BigDecimal("3"), new BigDecimal("100"), null, null, aggId, null));
        assertEquals(aggId, byId.holdingId());
        assertThrows(BadRequestException.class, () -> contributions.create(buy("2026-10-03", "NOPE")));
        assertThrows(BadRequestException.class, () -> contributions.create(new ContributionRequest(
                LocalDate.parse("2026-10-03"), 30000L, new BigDecimal("3"), new BigDecimal("100"), null, null, 9999L, null)));
    }

    @Test
    void updatingABuyKeepsItsFundUnlessAnotherIsNamed() {
        holdings.create(fund("AGG", "25"));
        ContributionResponse created = contributions.create(buy("2026-10-01", "AGG"));
        ContributionResponse kept = contributions.update(created.id(), buyWithoutFund("2026-10-02"));
        assertEquals("AGG", kept.ticker());
        ContributionResponse moved = contributions.update(created.id(), buy("2026-10-02", "VT"));
        assertEquals("VT", moved.ticker());
    }

    @Test
    void csvImportReadsAndExportsTheTickerColumn() {
        holdings.create(fund("AGG", "25"));
        String csv = "date,amount,shares,price,ticker\n2026-10-01,300.00,3,100,AGG\n2026-10-02,300.00,3,100,vt\n";
        ImportResult result = contributions.importCsv(csv);
        assertEquals(2, result.imported());
        String exported = contributions.exportCsv();
        assertTrue(exported.startsWith("date,amount,shares,price,fee,notes,ticker"), exported);
        assertTrue(exported.contains(",AGG"), exported);
        assertTrue(exported.contains(",VT"), exported);
    }

    @Test
    void csvImportReportsAnUnknownFundPerRow_andSavesNothing() {
        String csv = "date,amount,shares,price,ticker\n2026-10-01,300.00,3,100,VT\n2026-10-02,300.00,3,100,NOPE\n";
        ImportResult result = contributions.importCsv(csv);
        assertEquals(0, result.imported());
        assertEquals(1, result.errors().size());
        assertEquals(3, result.errors().get(0).line());
        assertTrue(result.errors().get(0).message().contains("NOPE"));
        assertTrue(contributions.list().isEmpty());
    }

    @Test
    void anOldCsvWithoutATickerStillImportsWhileThereIsOneFund() {
        ImportResult ok = contributions.importCsv("date,amount,shares,price\n2026-10-01,300.00,3,100\n");
        assertEquals(1, ok.imported());
        holdings.create(fund("AGG", "25"));
        ImportResult refused = contributions.importCsv("date,amount,shares,price\n2026-10-02,300.00,3,100\n");
        assertEquals(0, refused.imported());
        assertFalse(refused.errors().isEmpty());
    }
}
