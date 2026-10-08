package trademind.tm_server.invest.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the journal entries and learning notes that relate to a question, using PostgreSQL full-text
 * search (V27). Read-only. It only selects: the text it returns still has to be wrapped with
 * {@link UserContextRenderer#wrap} before it goes into a prompt.
 *
 * <p>Matching is "any of the question's words" (ranked, so rows that match more words come first), not
 * "all of them": a natural-language question has too many words for every one to appear in one entry.
 * Common words are dropped by the 'english' configuration. A small recency boost breaks ties, and the
 * newest entries are always included so questions about "lately" have something to read.
 *
 * <p>Soft-deleted rows are never returned. A source the caller switched off is never queried at all.
 */
@Service
@Transactional(readOnly = true)
public class RetrievalService {

    public static final int MAX_JOURNAL = 8;
    public static final int MAX_NOTES = 4;
    /** Newest entries added even when they do not match, so "lately" questions work. */
    public static final int RECENT_ENTRIES = 2;
    public static final int MAX_SNIPPET_CHARS = 600;
    public static final int MAX_TERMS = 12;

    /** How many times more candidates than we keep are fetched before the recency boost re-ranks them. */
    private static final int OVERFETCH = 3;
    private static final double RECENCY_WEIGHT = 0.25;
    private static final double RECENCY_HALF_LIFE_DAYS = 90.0;
    private static final int CONTEXT_BEFORE_MATCH = 150;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    public enum Kind {
        JOURNAL("J"), NOTE("N");

        private final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * One piece of the owner's text a feature may quote.
     *
     * @param title   notes only; null for journal entries
     * @param text    raw, unwrapped, at most {@link #MAX_SNIPPET_CHARS} characters (plus ellipses)
     * @param matched false for the always-included newest entries that did not match the question
     */
    public record Snippet(Kind kind, long id, LocalDate date, String title, String text, boolean matched) {
        /** The short stable name the model cites it by, such as J12 or N7. */
        public String handle() {
            return kind.prefix + id;
        }
    }

    public record Result(List<Snippet> journal, List<Snippet> notes) {
        public boolean isEmpty() {
            return journal.isEmpty() && notes.isEmpty();
        }
    }

    private record Candidate(long id, LocalDateTime createdAt, String title, String body, double rank) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public RetrievalService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Result search(String question, boolean includeJournal, boolean includeNotes) {
        List<String> terms = terms(question);
        String query = String.join(" or ", terms);
        List<Snippet> journal = includeJournal ? journal(query, terms) : List.of();
        List<Snippet> notes = includeNotes ? notes(query, terms) : List.of();
        return new Result(journal, notes);
    }

    // ---- queries ------------------------------------------------------------------------------

    private List<Snippet> journal(String query, List<String> terms) {
        List<Candidate> matches = query.isEmpty() ? List.of() : jdbc.query("""
                SELECT e.id, e.created_at, e.body, ts_rank_cd(e.search_vector, q.tsq) AS rank
                FROM invest_journal_entries e, websearch_to_tsquery('english', ?) AS q(tsq)
                WHERE e.deleted_at IS NULL AND e.search_vector @@ q.tsq
                ORDER BY rank DESC, e.created_at DESC
                LIMIT ?""",
                (rs, i) -> new Candidate(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(), null,
                        rs.getString("body"), rs.getDouble("rank")),
                query, MAX_JOURNAL * OVERFETCH);

        List<Snippet> out = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Candidate c : reRank(matches, MAX_JOURNAL)) {
            out.add(snippet(Kind.JOURNAL, c, true, terms));
            seen.add(c.id());
        }
        List<Candidate> newest = jdbc.query("""
                SELECT id, created_at, body FROM invest_journal_entries
                WHERE deleted_at IS NULL ORDER BY created_at DESC, id DESC LIMIT ?""",
                (rs, i) -> new Candidate(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(), null,
                        rs.getString("body"), 0),
                RECENT_ENTRIES);
        for (Candidate c : newest) {
            if (seen.add(c.id())) out.add(snippet(Kind.JOURNAL, c, false, terms));
        }
        return out;
    }

    private List<Snippet> notes(String query, List<String> terms) {
        if (query.isEmpty()) return List.of();
        List<Candidate> matches = jdbc.query("""
                SELECT n.id, n.created_at, n.title, n.body, ts_rank_cd(n.search_vector, q.tsq) AS rank
                FROM invest_learning_notes n, websearch_to_tsquery('english', ?) AS q(tsq)
                WHERE n.deleted_at IS NULL AND n.search_vector @@ q.tsq
                ORDER BY rank DESC, n.created_at DESC
                LIMIT ?""",
                (rs, i) -> new Candidate(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("title"), rs.getString("body"), rs.getDouble("rank")),
                query, MAX_NOTES * OVERFETCH);
        List<Snippet> out = new ArrayList<>();
        for (Candidate c : reRank(matches, MAX_NOTES)) out.add(snippet(Kind.NOTE, c, true, terms));
        return out;
    }

    // ---- ranking and excerpts (pure, unit-tested) ---------------------------------------------

    /** Text rank, nudged up for newer rows: a row from today scores 25% higher than an ancient one. */
    private List<Candidate> reRank(List<Candidate> candidates, int keep) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Candidate c) -> -boosted(c, now))
                        .thenComparing(Comparator.comparing(Candidate::createdAt).reversed())
                        .thenComparingLong(Candidate::id))
                .limit(keep)
                .toList();
    }

    static double boosted(Candidate c, LocalDateTime now) {
        double ageDays = Math.max(0, Duration.between(c.createdAt(), now).toHours() / 24.0);
        return c.rank() * (1 + RECENCY_WEIGHT / (1 + ageDays / RECENCY_HALF_LIFE_DAYS));
    }

    private static Snippet snippet(Kind kind, Candidate c, boolean matched, List<String> terms) {
        return new Snippet(kind, c.id(), c.createdAt().toLocalDate(), c.title(), excerpt(c.body(), terms), matched);
    }

    /**
     * Distinct lower-cased words of the question, at most {@link #MAX_TERMS}. Only letters and digits
     * survive, so nothing the owner types can become query syntax (quotes, minus, parentheses), and the
     * word "or" is dropped because it is the one word websearch_to_tsquery treats as an operator.
     */
    static List<String> terms(String question) {
        if (question == null) return List.of();
        Set<String> terms = new LinkedHashSet<>();
        Matcher m = WORD.matcher(question.toLowerCase(Locale.ROOT));
        while (m.find() && terms.size() < MAX_TERMS) {
            String word = m.group();
            if (!word.equals("or")) terms.add(word);
        }
        return List.copyOf(terms);
    }

    /**
     * At most {@link #MAX_SNIPPET_CHARS} characters of {@code text}, starting a little before the first
     * place a query word appears (so the evidence is not cut off in a long entry), with an ellipsis where
     * text was left out. Whitespace is kept as written; the caller flattens it when wrapping.
     */
    static String excerpt(String text, List<String> terms) {
        if (text == null) return "";
        String s = text.strip();
        if (s.length() <= MAX_SNIPPET_CHARS) return s;
        int first = -1;
        String lower = s.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term.length() < 3) continue;
            int at = lower.indexOf(term);
            if (at >= 0 && (first < 0 || at < first)) first = at;
        }
        int start = first <= CONTEXT_BEFORE_MATCH ? 0 : first - CONTEXT_BEFORE_MATCH;
        if (start > 0) { // begin at a word boundary rather than mid-word
            int space = s.indexOf(' ', start);
            if (space >= 0 && space - start < 20) start = space + 1;
        }
        int end = Math.min(s.length(), start + MAX_SNIPPET_CHARS);
        String cut = s.substring(start, end);
        return (start > 0 ? "…" : "") + cut.strip() + (end < s.length() ? "…" : "");
    }
}
