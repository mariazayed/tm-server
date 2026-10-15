package trademind.tm_server.invest.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.SettingsChange;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.SettingsChangeRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Saving the plan leaves a history of the three fields the plan rule is about, and only when they really change. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = SettingsChangeTest.TestApp.class)
@Import({SettingsService.class, trademind.tm_server.invest.holding.HoldingService.class})
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

class SettingsChangeTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    @Autowired SettingsService service;
    @Autowired InvestSettingsRepository settings;
    @Autowired SettingsChangeRepository changes;

    private InvestSettings current;

    @BeforeEach
    void readCurrent() {
        current = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
    }

    private SettingsRequest request(long amount, String ticker, int buyDay, Integer waitHours) {
        return new SettingsRequest(amount, current.getCurrency(), buyDay, ticker, current.getStartDate(), current.getTimezone(),
                false, null, 3, waitHours);
    }

    private SettingsRequest same() {
        return request(current.getMonthlyAmountCents(), current.getTicker(), current.getBuyDay(), null);
    }

    private List<SettingsChange> history() {
        return changes.findByDeletedAtIsNullOrderByCreatedAtAscIdAsc();
    }

    @Test
    void savingTheSamePlanAgainWritesNothing() {
        service.update(same());
        service.update(same());
        assertEquals(List.of(), history());
    }

    @Test
    void aChangedAmountIsRecordedWithOldAndNewValue() {
        long old = current.getMonthlyAmountCents();
        service.update(request(old + 5000, current.getTicker(), current.getBuyDay(), null));
        List<SettingsChange> rows = history();
        assertEquals(1, rows.size());
        assertEquals("MONTHLY_AMOUNT", rows.get(0).getFieldName());
        assertEquals(Long.toString(old), rows.get(0).getOldValue());
        assertEquals(Long.toString(old + 5000), rows.get(0).getNewValue());
        assertNotNull(rows.get(0).getCreatedAt());
    }

    @Test
    void theTickerIsComparedAfterNormalisingItsCase() {
        service.update(request(current.getMonthlyAmountCents(), current.getTicker().toLowerCase(), current.getBuyDay(), null));
        assertEquals(List.of(), history(), "vt is VT");
        service.update(request(current.getMonthlyAmountCents(), "vti", current.getBuyDay(), null));
        List<SettingsChange> rows = history();
        assertEquals(1, rows.size());
        assertEquals("TICKER", rows.get(0).getFieldName());
        assertEquals("VTI", rows.get(0).getNewValue());
    }

    @Test
    void fieldsChangedTogetherEachGetARow() {
        int newDay = current.getBuyDay() == 5 ? 6 : 5;
        service.update(request(current.getMonthlyAmountCents() + 100, "VTI", newDay, null));
        assertEquals(List.of("BUY_DAY", "MONTHLY_AMOUNT", "TICKER"),
                history().stream().map(SettingsChange::getFieldName).sorted().toList());
    }

    @Test
    void theWaitIsSavedWhenGivenAndKeptWhenLeftOutAndIsNotPartOfTheHistory() {
        InvestSettingsResponse changed = service.update(request(current.getMonthlyAmountCents(), current.getTicker(), current.getBuyDay(), 48));
        assertEquals(48, changed.ruleWaitHours());
        assertEquals(48, service.update(same()).ruleWaitHours(), "left out keeps 48");
        assertEquals(List.of(), history());
    }

    @Test
    void aRejectedSaveLeavesNoHistory() {
        SettingsRequest bad = new SettingsRequest(current.getMonthlyAmountCents() + 1, "ZZZ", (int) current.getBuyDay(), current.getTicker(),
                current.getStartDate(), current.getTimezone(), false, null, 3, null);
        assertThrows(BadRequestException.class, () -> service.update(bad));
        assertEquals(List.of(), history());
    }
}
