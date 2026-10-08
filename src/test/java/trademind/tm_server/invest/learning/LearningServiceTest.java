package trademind.tm_server.invest.learning;

import jakarta.persistence.EntityManager;
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
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.enums.SourceType;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.TagService;
import trademind.tm_server.invest.learning.LearningDtos.CardRequest;
import trademind.tm_server.invest.learning.LearningDtos.CardResponse;
import trademind.tm_server.invest.learning.LearningDtos.NotePage;
import trademind.tm_server.invest.learning.LearningDtos.NoteRequest;
import trademind.tm_server.invest.learning.LearningDtos.NoteResponse;
import trademind.tm_server.invest.learning.LearningDtos.SourceRequest;
import trademind.tm_server.invest.learning.LearningDtos.SourceResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

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
@ContextConfiguration(classes = LearningServiceTest.TestApp.class)
@Import({LearningService.class, FlashcardService.class, TagService.class, LearningServiceTest.FixedClock.class})
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
class LearningServiceTest {

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

    @Autowired LearningService learning;
    @Autowired FlashcardService flashcards;
    @Autowired EntityManager em;

    private SourceResponse source(String title) {
        return learning.createSource(new SourceRequest(title, SourceType.BOOK, null, null, null));
    }

    private NoteResponse note(String title, String body, Long sourceId, String... tags) {
        return learning.createNote(new NoteRequest(title, body, sourceId, List.of(tags)));
    }

    private static LearningService.NoteFilter noFilter() {
        return new LearningService.NoteFilter(null, null, null);
    }

    // ---- sources ------------------------------------------------------------------------------

    @Test
    void sourceLifecycleAndStatusFilter() {
        SourceResponse created = learning.createSource(new SourceRequest("  The Simple Path to Wealth ",
                SourceType.BOOK, " https://example.com/book ", null, "Chapter 1 first"));
        assertEquals("The Simple Path to Wealth", created.title());
        assertEquals(SourceStatus.TO_DO, created.status()); // default
        assertEquals("https://example.com/book", created.url());

        SourceResponse started = learning.updateSource(created.id(), new SourceRequest(
                "The Simple Path to Wealth", SourceType.BOOK, null, SourceStatus.IN_PROGRESS, null));
        assertEquals(SourceStatus.IN_PROGRESS, started.status());
        assertNull(started.url());

        source("Done one");
        assertEquals(1, learning.listSources(SourceStatus.IN_PROGRESS).size());
        assertEquals(2, learning.listSources(null).size());

        learning.deleteSource(created.id());
        assertEquals(1, learning.listSources(null).size());
        assertThrows(ResourceNotFoundException.class, () -> learning.getSource(created.id()));
    }

    @Test
    void sourcesRejectUnsafeLinks() {
        assertThrows(BadRequestException.class, () -> learning.createSource(
                new SourceRequest("x", SourceType.ARTICLE, "javascript:alert(1)", null, null)));
        assertEquals(0, learning.listSources(null).size());
    }

    // ---- notes --------------------------------------------------------------------------------

    @Test
    void noteLifecycleWithSourceAndTags() {
        SourceResponse src = source("Book");
        NoteResponse n = note("DCA", "Invest on a schedule.", src.id(), "Learning", "#Basics");
        assertEquals("Book", n.sourceTitle());
        assertEquals(List.of("basics", "learning"), n.tags());

        NoteResponse edited = learning.updateNote(n.id(), new NoteRequest("DCA", "Edited.", null, List.of("learning")));
        assertEquals("Edited.", edited.body());
        assertNull(edited.sourceId());
        assertEquals(List.of("learning"), edited.tags());

        learning.deleteNote(n.id());
        assertThrows(ResourceNotFoundException.class, () -> learning.getNote(n.id()));
        assertEquals(0, learning.listNotes(noFilter(), 0, 10).total());
    }

    @Test
    void noteMustPointAtALiveSource() {
        SourceResponse src = source("Gone soon");
        learning.deleteSource(src.id());
        assertThrows(BadRequestException.class,
                () -> note("Orphan", "x", src.id()));
        assertThrows(BadRequestException.class, () -> note("Orphan", "x", 99999L));
    }

    @Test
    void deletingASourceDetachesItFromItsNotesInResponses() {
        SourceResponse src = source("Soon deleted");
        NoteResponse n = note("Kept", "Still here", src.id());
        learning.deleteSource(src.id());
        em.flush(); // persist the soft delete before dropping the cache
        em.clear();
        NoteResponse after = learning.getNote(n.id());
        assertNull(after.sourceId());
        assertNull(after.sourceTitle());
    }

    @Test
    void notesFilterBySourceTagAndText() {
        SourceResponse a = source("A");
        SourceResponse b = source("B");
        note("Expense ratios", "Fees compound too", a.id(), "costs");
        note("DCA", "Fixed amount 100% on schedule", b.id(), "costs", "habits");
        note("Rebalancing", "Not needed with one fund", b.id());

        assertEquals(1, learning.listNotes(new LearningService.NoteFilter(a.id(), null, null), 0, 10).total());
        assertEquals(2, learning.listNotes(new LearningService.NoteFilter(b.id(), null, null), 0, 10).total());
        assertEquals(2, learning.listNotes(new LearningService.NoteFilter(null, "#Costs", null), 0, 10).total());
        assertEquals(1, learning.listNotes(new LearningService.NoteFilter(b.id(), "costs", null), 0, 10).total());
        assertEquals(1, learning.listNotes(new LearningService.NoteFilter(null, null, "REBALANC"), 0, 10).total()); // title
        assertEquals(1, learning.listNotes(new LearningService.NoteFilter(null, null, "compound"), 0, 10).total()); // body
        assertEquals(1, learning.listNotes(new LearningService.NoteFilter(null, null, "%"), 0, 10).total()); // literal %
        assertEquals(0, learning.listNotes(new LearningService.NoteFilter(null, null, "zzz"), 0, 10).total());
    }

    @Test
    void notePagingHydratesTagsAndSources() {
        SourceResponse src = source("Src");
        for (int i = 1; i <= 3; i++) note("n" + i, "body " + i, src.id(), "t");
        NotePage first = learning.listNotes(noFilter(), 0, 2);
        assertEquals(3, first.total());
        assertTrue(first.hasMore());
        assertEquals(List.of("t"), first.items().get(0).tags());
        assertEquals("Src", first.items().get(0).sourceTitle());
        assertEquals(1, learning.listNotes(noFilter(), 1, 2).items().size());
        assertThrows(BadRequestException.class, () -> learning.listNotes(noFilter(), 0, 500));
    }

    @Test
    void noteTagUsageCountsLiveNotesOnly() {
        NoteResponse n = note("one", "x", null, "alpha");
        note("two", "y", null, "alpha", "beta");
        var usage = learning.noteTags();
        assertEquals(2, usage.stream().filter(t -> t.name().equals("alpha")).findFirst().orElseThrow().count());
        learning.deleteNote(n.id());
        assertEquals(1, learning.noteTags().stream().filter(t -> t.name().equals("alpha")).findFirst().orElseThrow().count());
    }

    // ---- flashcards ---------------------------------------------------------------------------

    private CardResponse card(String front, Long noteId) {
        return flashcards.create(new CardRequest(front, "answer", noteId));
    }

    @Test
    void newCardsAreDueTodayAndShowInTheQueue() {
        CardResponse c = card("What is DCA?", null);
        assertEquals(LocalDate.parse("2026-10-15"), c.dueOn());
        assertEquals(0, c.repetitions());
        assertEquals(1, flashcards.due(10).size());
        var stats = flashcards.stats();
        assertEquals(1, stats.total());
        assertEquals(1, stats.due());
        assertEquals(1, stats.newCards());
        assertEquals(0, stats.reviewedToday());
    }

    @Test
    void reviewingReschedulesAndUpdatesStats() {
        CardResponse c = card("Q", null);
        CardResponse good = flashcards.review(c.id(), Sm2Scheduler.Grade.GOOD);
        assertEquals(1, good.repetitions());
        assertEquals(1, good.intervalDays());
        assertEquals(LocalDate.parse("2026-10-16"), good.dueOn());
        assertEquals(LocalDate.parse("2026-10-15"), good.lastReviewedOn());
        assertEquals(0, flashcards.due(10).size()); // gone from today's queue

        var stats = flashcards.stats();
        assertEquals(0, stats.due());
        assertEquals(0, stats.newCards());
        assertEquals(1, stats.reviewedToday());

        // The same card can be graded again the same day (e.g. a misclick): the ladder just advances.
        CardResponse again = flashcards.review(c.id(), Sm2Scheduler.Grade.GOOD);
        assertEquals(6, again.intervalDays());
        assertEquals(LocalDate.parse("2026-10-21"), again.dueOn());
    }

    @Test
    void failingACardBringsItBackTomorrowAndCountsALapse() {
        CardResponse c = card("Q", null);
        flashcards.review(c.id(), Sm2Scheduler.Grade.GOOD);
        CardResponse failed = flashcards.review(c.id(), Sm2Scheduler.Grade.AGAIN);
        assertEquals(0, failed.repetitions());
        assertEquals(1, failed.lapses());
        assertEquals(LocalDate.parse("2026-10-16"), failed.dueOn());
    }

    @Test
    void queueIsMostOverdueFirstAndRespectsTheLimit() {
        CardResponse fresh = card("fresh", null);
        CardResponse overdue = card("overdue", null);
        CardResponse future = card("future", null);
        em.createNativeQuery("update invest_flashcards set due_on = date '2026-10-01' where id = :id")
                .setParameter("id", overdue.id()).executeUpdate();
        em.createNativeQuery("update invest_flashcards set due_on = date '2026-10-20' where id = :id")
                .setParameter("id", future.id()).executeUpdate();
        em.flush();
        em.clear();

        List<CardResponse> queue = flashcards.due(10);
        assertEquals(List.of(overdue.id(), fresh.id()), queue.stream().map(CardResponse::id).toList());
        assertEquals(List.of(overdue.id()), flashcards.due(1).stream().map(CardResponse::id).toList());
        assertEquals(2, flashcards.stats().due());
        assertThrows(BadRequestException.class, () -> flashcards.due(0));
        assertThrows(BadRequestException.class, () -> flashcards.due(101));
    }

    @Test
    void editingTextKeepsTheSchedule() {
        CardResponse c = card("Q", null);
        flashcards.review(c.id(), Sm2Scheduler.Grade.GOOD);
        CardResponse edited = flashcards.update(c.id(), new CardRequest("Q2", "A2", null));
        assertEquals("Q2", edited.front());
        assertEquals(1, edited.repetitions());
        assertEquals(LocalDate.parse("2026-10-16"), edited.dueOn());
    }

    @Test
    void cardsLinkToLiveNotesAndSurviveNoteDeletion() {
        NoteResponse n = note("DCA", "Body", null);
        CardResponse c = card("Q", n.id());
        assertEquals(n.id(), c.noteId());
        assertEquals(1, flashcards.list(n.id()).size());

        assertThrows(BadRequestException.class, () -> card("Q", 99999L));

        learning.deleteNote(n.id());
        em.flush();
        em.clear();
        assertNull(flashcards.list(null).get(0).noteId()); // card stays, link hidden
        assertThrows(BadRequestException.class, () -> card("Q", n.id()));
    }

    @Test
    void deletedCardsDisappearEverywhere() {
        CardResponse c = card("Q", null);
        flashcards.delete(c.id());
        assertEquals(0, flashcards.list(null).size());
        assertEquals(0, flashcards.due(10).size());
        assertEquals(0, flashcards.stats().total());
        assertThrows(ResourceNotFoundException.class, () -> flashcards.review(c.id(), Sm2Scheduler.Grade.GOOD));
    }
}
