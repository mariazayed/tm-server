package trademind.tm_server.invest.price;

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
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.performance.PerformanceResponse;
import trademind.tm_server.invest.performance.PerformanceService;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs a scratch PostgreSQL; see InvestSchemaTest. Fixed clock: 2026-10-15 UTC. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PriceAndPerformanceServiceTest.TestApp.class)
@Import({PriceService.class, PerformanceService.class, PriceAndPerformanceServiceTest.Fakes.class})
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:invest-schema-reset.sql,"
                + "classpath:db/migration/V22__create_invest_tables.sql,"
                + "classpath:db/migration/V23__create_invest_flashcards.sql"
})
class PriceAndPerformanceServiceTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    /** Records what was asked of the provider and serves scripted closes. */
    static class FakeProvider implements PriceProvider {
        final List<LocalDate[]> calls = new ArrayList<>();
        List<DailyClose> response = List.of();
        RuntimeException failure;

        @Override
        public List<DailyClose> fetchDailyCloses(String ticker, LocalDate from, LocalDate to) {
            calls.add(new LocalDate[] {from, to});
            if (failure != null) throw failure;
            return response.stream().filter(c -> !c.date().isBefore(from) && !c.date().isAfter(to)).toList();
        }
    }

    static class Fakes {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        FakeProvider fakeProvider() {
            return new FakeProvider();
        }
    }

    @Autowired PriceService prices;
    @Autowired PerformanceService performance;
    @Autowired FakeProvider provider;
    @Autowired InvestSettingsRepository settings;
    @Autowired ContributionRepository contributions;
    @Autowired PriceQuoteRepository quotes;

    private static PriceProvider.DailyClose close(String date, String value) {
        return new PriceProvider.DailyClose(LocalDate.parse(date), new BigDecimal(value));
    }

    @BeforeEach
    void plan() {
        provider.calls.clear();
        provider.failure = null;
        provider.response = List.of();
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setStartDate(LocalDate.parse("2026-08-01"));
        settings.saveAndFlush(s);
    }

    private void buy(String date, long cents, String shares, String price) {
        Contribution c = new Contribution();
        c.setContributedOn(LocalDate.parse(date));
        c.setAmountCents(cents);
        c.setShares(new BigDecimal(shares));
        c.setPrice(new BigDecimal(price));
        contributions.saveAndFlush(c);
    }

    @Test
    void firstRefreshBackfillsFromBeforeThePlanStart() {
        buy("2026-07-20", 30000, "3", "100"); // earlier than the plan start date
        provider.response = List.of(close("2026-07-20", "100"), close("2026-10-14", "130"));

        PriceService.RefreshResult r = prices.refresh();

        assertEquals(LocalDate.parse("2026-07-15"), provider.calls.get(0)[0]); // earliest buy minus 5 days
        assertEquals(LocalDate.parse("2026-10-15"), provider.calls.get(0)[1]);
        assertEquals(2, r.inserted());
        assertEquals(LocalDate.parse("2026-10-14"), r.latestDate());
        assertNull(r.error());
    }

    @Test
    void laterRefreshesOverlapRecentDaysInsertNewOnesAndCorrectChangedCloses() {
        provider.response = List.of(close("2026-10-08", "128"), close("2026-10-09", "129"));
        prices.refresh();

        provider.response = List.of(close("2026-10-08", "128"), close("2026-10-09", "129.5"), close("2026-10-12", "131"));
        PriceService.RefreshResult r = prices.refresh();

        assertEquals(LocalDate.parse("2026-10-04"), provider.calls.get(1)[0]); // latest 10-09 minus 5
        assertEquals(1, r.inserted());
        assertEquals(1, r.updated());
        assertEquals(3, quotes.count());

        PriceService.RefreshResult again = prices.refresh(); // nothing new: idempotent
        assertEquals(0, again.inserted());
        assertEquals(0, again.updated());
        assertEquals(3, quotes.count());
    }

    @Test
    void aProviderFailureKeepsTheCacheAndReportsTheError() {
        provider.response = List.of(close("2026-10-09", "129"));
        prices.refresh();

        provider.failure = new PriceProvider.PriceProviderException("HTTP 429");
        PriceService.RefreshResult r = prices.refresh();

        assertEquals("HTTP 429", r.error());
        assertEquals(LocalDate.parse("2026-10-09"), r.latestDate());
        assertEquals(1, quotes.count());
    }

    @Test
    void performanceUsesCachedClosesAndFiltersTheSeriesByRange() {
        buy("2026-05-01", 10000, "1", "100");
        buy("2026-08-01", 30000, "3", "100");
        buy("2026-09-01", 30000, "2.5", "120");
        provider.response = List.of(close("2026-05-01", "100"), close("2026-08-01", "100"),
                close("2026-09-01", "120"), close("2026-10-14", "130"));
        prices.refresh();

        PerformanceResponse all = performance.performance("ALL");
        assertEquals(70000, all.investedCents());
        assertEquals(84500, all.valueCents()); // 6.5 shares * 130
        assertEquals(0, new BigDecimal("130").compareTo(all.currentPrice()));
        assertTrue(all.priceFromCache());
        assertEquals(4, all.series().size());

        // 3M trims the chart only (cutoff 2026-07-15); totals still cover everything.
        PerformanceResponse recent = performance.performance("3m");
        assertEquals("3M", recent.range());
        assertEquals(3, recent.series().size());
        assertEquals(70000, recent.investedCents());
        // The trimmed chart still starts from the cumulative total, including the earlier May buy.
        assertEquals(LocalDate.parse("2026-08-01"), recent.series().get(0).date());
        assertEquals(40000, recent.series().get(0).investedCents());

        assertThrows(BadRequestException.class, () -> performance.performance("5Y"));
    }

    @Test
    void performanceWithoutCachedPricesFallsBackToTheLastBuy() {
        buy("2026-09-01", 30000, "2.5", "120");
        PerformanceResponse p = performance.performance("ALL");
        assertEquals(false, p.priceFromCache());
        assertEquals(30000, p.valueCents());
        assertEquals(1, p.series().size());
    }
}
