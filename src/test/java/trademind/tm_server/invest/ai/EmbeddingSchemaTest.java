package trademind.tm_server.invest.ai;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.repository.EmbeddingRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shape of the embeddings table, plus an audit that every table keeps the columns AI features rely on. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = EmbeddingSchemaTest.TestApp.class)
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
class EmbeddingSchemaTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = {InvestSettings.class, EmbeddingRecord.class})
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    @Autowired EmbeddingRepository embeddings;
    @Autowired EntityManager em;

    private static EmbeddingRecord record(EmbeddingSource type, long sourceId, String model, float[] vector) {
        EmbeddingRecord r = new EmbeddingRecord();
        r.setSourceType(type);
        r.setSourceId(sourceId);
        r.setModel(model);
        r.setDimensions(vector.length);
        r.setVector(vector);
        r.setContentHash("a".repeat(64));
        return r;
    }

    @Test
    void aVectorRoundTripsAndTimestampsAreSet() {
        float[] vector = {0.1f, -0.25f, 3.5f, 0f};
        EmbeddingRecord saved = embeddings.saveAndFlush(record(EmbeddingSource.JOURNAL, 7, "embed-test", vector));
        em.clear();

        EmbeddingRecord loaded = embeddings.findBySourceTypeAndSourceIdAndModel(EmbeddingSource.JOURNAL, 7L, "embed-test").orElseThrow();
        assertArrayEquals(vector, loaded.getVector());
        assertEquals(4, loaded.getDimensions());
        assertEquals("a".repeat(64), loaded.getContentHash());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(saved.getId(), loaded.getId());
    }

    @Test
    void theSameThingCanBeEmbeddedByDifferentModelsButNotTwiceByOne() {
        embeddings.saveAndFlush(record(EmbeddingSource.NOTE, 1, "model-a", new float[] {1f, 2f}));
        embeddings.saveAndFlush(record(EmbeddingSource.NOTE, 1, "model-b", new float[] {1f, 2f, 3f}));
        embeddings.saveAndFlush(record(EmbeddingSource.JOURNAL, 1, "model-a", new float[] {1f, 2f}));
        assertEquals(1, embeddings.findByModel("model-b").size());

        assertThrows(DataIntegrityViolationException.class,
                () -> embeddings.saveAndFlush(record(EmbeddingSource.NOTE, 1, "model-a", new float[] {9f, 9f})));
    }

    @Test
    void theContentHashMustBeAFullSha256Hex() {
        EmbeddingRecord shortHash = record(EmbeddingSource.NOTE, 6, "model-a", new float[] {1f});
        shortHash.setContentHash("abc");
        assertThrows(DataIntegrityViolationException.class, () -> embeddings.saveAndFlush(shortHash));
    }

    @Test
    void theDeclaredDimensionsMustMatchTheVector() {
        EmbeddingRecord lying = record(EmbeddingSource.NOTE, 5, "model-a", new float[] {1f, 2f, 3f});
        lying.setDimensions(2);
        assertThrows(DataIntegrityViolationException.class, () -> embeddings.saveAndFlush(lying));
    }

    // ---- schema audit -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> columnsByTable() {
        List<Object[]> rows = em.createNativeQuery("""
                select table_name, column_name from information_schema.columns
                where table_schema = current_schema() and table_name like 'invest\\_%' escape '\\'""").getResultList();
        return rows.stream().collect(Collectors.groupingBy(r -> (String) r[0],
                Collectors.mapping(r -> (String) r[1], Collectors.toSet())));
    }

    @Test
    void everyUserDataTableHasAnIdAndFullAuditColumns() {
        Map<String, Set<String>> columns = columnsByTable();
        for (String table : List.of("invest_settings", "invest_contributions", "invest_tags", "invest_journal_entries",
                "invest_rules", "invest_sources", "invest_learning_notes", "invest_flashcards")) {
            Set<String> cols = columns.get(table);
            assertNotNull(cols, table + " is missing");
            for (String required : List.of("id", "created_at", "updated_at", "deleted_at", "notes")) {
                assertTrue(cols.contains(required), table + " has no " + required);
            }
        }
    }

    @Test
    void everyOtherTableStillHasAStableKey() {
        Map<String, Set<String>> columns = columnsByTable();
        assertTrue(columns.get("invest_prices").containsAll(Set.of("id", "ticker", "price_date")));
        assertTrue(columns.get("invest_reminder_log").containsAll(Set.of("id", "sent_at")));
        assertTrue(columns.get("invest_ai_usage").containsAll(Set.of("id", "usage_date", "feature", "calls")));
        assertTrue(columns.get("invest_embeddings").containsAll(Set.of("id", "created_at", "updated_at", "content_hash")));
        assertTrue(columns.get("invest_journal_entry_tags").containsAll(Set.of("entry_id", "tag_id")));
        assertTrue(columns.get("invest_learning_note_tags").containsAll(Set.of("note_id", "tag_id")));
    }

    @Test
    void everyTableIsCoveredByThisAudit() {
        // A new invest_ table must be added to the lists above so it is audited too.
        assertEquals(Set.of("invest_settings", "invest_contributions", "invest_tags", "invest_journal_entries",
                "invest_rules", "invest_sources", "invest_learning_notes", "invest_flashcards", "invest_prices",
                "invest_reminder_log", "invest_embeddings", "invest_ai_usage", "invest_journal_entry_tags", "invest_learning_note_tags"),
                columnsByTable().keySet());
    }

    @Test
    void idsAreNeverReusedAfterADelete() {
        EmbeddingRecord first = embeddings.saveAndFlush(record(EmbeddingSource.NOTE, 1, "m", new float[] {1f}));
        embeddings.deleteBySourceTypeAndSourceId(EmbeddingSource.NOTE, 1L);
        em.flush();
        EmbeddingRecord second = embeddings.saveAndFlush(record(EmbeddingSource.NOTE, 1, "m", new float[] {1f}));
        assertTrue(second.getId() > first.getId(), "a bigserial id is never handed out twice");
    }
}
