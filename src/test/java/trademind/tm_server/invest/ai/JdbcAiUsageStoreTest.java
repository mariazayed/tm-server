package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The real counting SQL: upsert, per-day isolation, token sums and the table's own constraints. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = JdbcAiUsageStoreTest.TestApp.class)
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.datasource.hikari.maximum-pool-size=2", // every cached test context holds its own pool; the default 10 each exhausts max_connections=100
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
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
class JdbcAiUsageStoreTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired JdbcTemplate jdbc;

    private static final LocalDate DAY = LocalDate.parse("2026-10-06");

    @Test
    void callsAccumulatePerFeatureAndPerDay() {
        JdbcAiUsageStore store = new JdbcAiUsageStore(jdbc);
        store.recordCall(DAY, AiFeature.CHAT);
        store.recordCall(DAY, AiFeature.CHAT);
        store.recordCall(DAY, AiFeature.RULE_CHECK);
        store.recordCall(DAY.plusDays(1), AiFeature.CHAT);

        AiUsageStore.Totals today = store.totals(DAY);
        assertEquals(3, today.calls());
        assertEquals(2L, today.callsByFeature().get("CHAT"));
        assertEquals(1L, today.callsByFeature().get("RULE_CHECK"));
        assertEquals(1, store.totals(DAY.plusDays(1)).calls());
        assertEquals(0, store.totals(DAY.minusDays(1)).calls());
    }

    @Test
    void tokensAreAddedToTheRightRow() {
        JdbcAiUsageStore store = new JdbcAiUsageStore(jdbc);
        store.recordCall(DAY, AiFeature.TAG_SUGGESTIONS);
        store.addTokens(DAY, AiFeature.TAG_SUGGESTIONS, 100, 20);
        store.addTokens(DAY, AiFeature.TAG_SUGGESTIONS, 50, 5);
        AiUsageStore.Totals t = store.totals(DAY);
        assertEquals(150, t.inputTokens());
        assertEquals(25, t.outputTokens());
        assertEquals(1, t.calls());
    }

    @Test
    void negativeCountsAreRejectedByTheTable() {
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO invest_ai_usage (usage_date, feature, calls) VALUES (?, 'X', -1)", java.sql.Date.valueOf(DAY)));
    }
}
