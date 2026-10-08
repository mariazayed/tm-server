package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.invest.ai.RetrievalService.Kind;
import trademind.tm_server.invest.ai.RetrievalService.Result;
import trademind.tm_server.invest.ai.RetrievalService.Snippet;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retrieval against real PostgreSQL full-text search (needs INVEST_TEST_DB_URL, like the other database tests). */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RetrievalServiceTest.TestApp.class)
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
                + "classpath:db/migration/V27__invest_fulltext_search.sql"
})
class RetrievalServiceTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired JdbcTemplate jdbc;

    private RetrievalService retrieval;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC);
        retrieval = new RetrievalService(jdbc, clock);
    }

    private long entry(String createdAt, String body) {
        return jdbc.queryForObject("INSERT INTO invest_journal_entries (entry_type, body, created_at) "
                + "VALUES ('QUICK', ?, ?::timestamp) RETURNING id", Long.class, body, createdAt);
    }

    private long note(String createdAt, String title, String body) {
        return jdbc.queryForObject("INSERT INTO invest_learning_notes (title, body, created_at) "
                + "VALUES (?, ?, ?::timestamp) RETURNING id", Long.class, title, body, createdAt);
    }

    private static List<Long> ids(List<Snippet> snippets) {
        return snippets.stream().map(Snippet::id).toList();
    }

    @Test
    void wordsAreStemmedSoWorryFindsWorried() {
        long scared = entry("2026-03-04 09:00", "I was worried and scared when the market dropped.");
        entry("2026-03-05 09:00", "Bought on the usual day, felt calm.");
        Snippet first = retrieval.search("When did I worry about the market?", true, true).journal().get(0);
        assertEquals(scared, first.id());
        assertTrue(first.matched());
        assertEquals("J" + scared, first.handle());
        assertEquals(LocalDate.parse("2026-03-04"), first.date());
    }

    @Test
    void anyWordCanMatchSoALongQuestionStillFindsThings() {
        long id = entry("2026-03-04 09:00", "The crash scared me.");
        entry("2026-09-01 09:00", "Quiet month one."); // two newer entries, so the match is not just "recent"
        entry("2026-10-01 09:00", "Quiet month two.");
        Result r = retrieval.search("What did I write the last time there was a big crash in the stock market?", true, false);
        Snippet found = r.journal().stream().filter(sn -> sn.id() == id).findFirst().orElseThrow();
        assertTrue(found.matched(), "matched on the single word 'crash' although most question words are absent");
    }

    @Test
    void rowsMatchingMoreOfTheWordsComeFirst() {
        long one = entry("2026-01-01 09:00", "The crash was loud.");
        entry("2026-09-01 09:00", "Quiet month one.");
        entry("2026-10-01 09:00", "Quiet month two.");
        long three = entry("2026-01-02 09:00", "The crash made me panic and I nearly sold everything.");
        List<Snippet> out = retrieval.search("crash panic sold", true, false).journal();
        assertEquals(three, out.get(0).id());
        Snippet partial = out.stream().filter(sn -> sn.id() == one).findFirst().orElseThrow();
        assertTrue(partial.matched(), "a row matching only one of the words is still found");
    }

    @Test
    void ofTwoEqualMatchesTheNewerOneWins() {
        long old = entry("2024-01-10 09:00", "I felt anxious about prices.");
        long recent = entry("2026-09-30 09:00", "I felt anxious about prices.");
        List<Snippet> out = retrieval.search("anxious prices", true, false).journal();
        assertEquals(recent, out.get(0).id());
        assertEquals(old, out.get(1).id());
    }

    @Test
    void theNewestEntriesAreIncludedEvenWhenTheyDoNotMatch() {
        long match = entry("2025-01-01 09:00", "Remember the inflation scare.");
        entry("2026-08-01 09:00", "Quiet month one.");
        long n2 = entry("2026-09-01 09:00", "Quiet month two.");
        long n3 = entry("2026-10-01 09:00", "Quiet month three.");
        List<Snippet> out = retrieval.search("inflation", true, false).journal();
        assertEquals(List.of(match, n3, n2), ids(out));
        assertTrue(out.get(0).matched());
        assertFalse(out.get(1).matched());
        assertFalse(out.get(2).matched());
    }

    @Test
    void aNewestEntryThatAlsoMatchesIsNotListedTwice() {
        long id = entry("2026-10-01 09:00", "Inflation again.");
        entry("2026-09-01 09:00", "Nothing special.");
        List<Snippet> out = retrieval.search("inflation", true, false).journal();
        assertEquals(1, ids(out).stream().filter(i -> i == id).count());
        assertTrue(out.stream().filter(s -> s.id() == id).findFirst().orElseThrow().matched());
    }

    @Test
    void deletedRowsNeverComeBackAsMatchesOrAsNewest() {
        long gone = entry("2026-10-02 09:00", "Secret panic entry.");
        long kept = entry("2026-09-02 09:00", "Another panic entry.");
        long goneNote = note("2026-10-02 09:00", "Panic", "Deleted note about panic.");
        jdbc.update("UPDATE invest_journal_entries SET deleted_at = now() WHERE id = ?", gone);
        jdbc.update("UPDATE invest_learning_notes SET deleted_at = now() WHERE id = ?", goneNote);
        Result r = retrieval.search("panic", true, true);
        assertEquals(List.of(kept), ids(r.journal()));
        assertTrue(r.notes().isEmpty());
    }

    @Test
    void aSourceThatIsSwitchedOffIsNotReadAtAll() {
        entry("2026-10-01 09:00", "Journal about inflation.");
        note("2026-10-01 09:00", "Inflation", "Note about inflation.");
        Result noJournal = retrieval.search("inflation", false, true);
        assertTrue(noJournal.journal().isEmpty(), "not even the newest entry");
        assertEquals(1, noJournal.notes().size());
        Result noNotes = retrieval.search("inflation", true, false);
        assertTrue(noNotes.notes().isEmpty());
        assertEquals(1, noNotes.journal().size());
        assertTrue(retrieval.search("inflation", false, false).isEmpty());
    }

    @Test
    void aQuestionOfOnlyCommonWordsMatchesNothingButStillGetsTheNewestEntries() {
        long newest = entry("2026-10-01 09:00", "Something.");
        note("2026-10-01 09:00", "Title", "Body.");
        Result r = retrieval.search("what is the of and to", true, true);
        assertEquals(List.of(newest), ids(r.journal()));
        assertFalse(r.journal().get(0).matched());
        assertTrue(r.notes().isEmpty());
    }

    @Test
    void hostileInputIsJustWordsAndNeverBreaksTheQuery() {
        entry("2026-10-01 09:00", "A calm month.");
        for (String q : new String[] {"'); DROP TABLE invest_journal_entries; --", "\"unterminated", "a & b | !c (d", "-- or or or",
                "' OR '1'='1", "\\", "<script>alert(1)</script>", ""}) {
            retrieval.search(q, true, true); // must not throw
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM invest_journal_entries", Integer.class));
    }

    @Test
    void aLongEntryIsCutAroundTheMatch() {
        String body = "filler ".repeat(400) + "the crash scared me badly " + "tail ".repeat(400);
        long id = entry("2026-05-01 09:00", body);
        Snippet s = retrieval.search("crash", true, false).journal().get(0);
        assertEquals(id, s.id());
        assertTrue(s.text().contains("crash scared me"));
        assertTrue(s.text().length() <= RetrievalService.MAX_SNIPPET_CHARS + 2);
    }

    @Test
    void thereAreCapsOnHowMuchComesBack() {
        for (int i = 0; i < 12; i++) {
            entry("2026-0" + (i % 9 + 1) + "-01 09:00", "Inflation worry number " + i);
            note("2026-0" + (i % 9 + 1) + "-01 09:00", "Inflation " + i, "More on inflation.");
        }
        Result r = retrieval.search("inflation", true, true);
        long matched = r.journal().stream().filter(Snippet::matched).count();
        assertEquals(RetrievalService.MAX_JOURNAL, matched);
        assertTrue(r.journal().size() <= RetrievalService.MAX_JOURNAL + RetrievalService.RECENT_ENTRIES);
        assertEquals(RetrievalService.MAX_NOTES, r.notes().size());
    }

    @Test
    void aWordInTheTitleOutranksTheSameWordInTheBody() {
        long inBody = note("2026-06-01 09:00", "Weekly reading", "Some thoughts on inflation and rates.");
        long inTitle = note("2026-06-01 09:00", "Inflation", "Some thoughts on rates and prices.");
        List<Snippet> out = retrieval.search("inflation", false, true).notes();
        assertEquals(List.of(inTitle, inBody), ids(out));
        assertEquals("N" + inTitle, out.get(0).handle());
        assertEquals("Inflation", out.get(0).title());
        assertEquals(Kind.NOTE, out.get(0).kind());
    }

    @Test
    void editingAnEntryMovesItInTheIndexWithoutAnyApplicationCode() {
        long id = entry("2026-06-01 09:00", "Nothing about prices.");
        assertTrue(retrieval.search("volatility", true, false).journal().stream().noneMatch(Snippet::matched));
        jdbc.update("UPDATE invest_journal_entries SET body = 'Volatility bothered me.' WHERE id = ?", id);
        assertTrue(retrieval.search("volatility", true, false).journal().get(0).matched());
    }

    @Test
    void theIndexesExist() {
        List<String> names = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename LIKE 'invest\\_%'", String.class);
        assertTrue(names.contains("idx_invest_journal_entries_search"));
        assertTrue(names.contains("idx_invest_learning_notes_search"));
        assertNotNull(jdbc.queryForObject("SELECT search_vector::text FROM (SELECT to_tsvector('english','x') AS search_vector) t", String.class));
    }
}
