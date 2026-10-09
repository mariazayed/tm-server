package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of retrieval: turning a question into search words, and cutting a snippet. */
class RetrievalTextTest {

    @Test
    void termsAreDistinctLowerCaseWords() {
        assertEquals(List.of("what", "did", "i", "write", "about", "fear"),
                RetrievalService.terms("What did I write about FEAR? Fear!"));
    }

    @Test
    void punctuationAndQueryOperatorsCannotSurvive() {
        List<String> terms = RetrievalService.terms("\"quoted\" -minus (paren) a&b | c' ; DROP TABLE x");
        assertEquals(List.of("quoted", "minus", "paren", "a", "b", "c", "drop", "table", "x"), terms);
        for (String t : terms) assertTrue(t.matches("[\\p{L}\\p{N}]+"), t);
    }

    @Test
    void theWordOrIsDroppedBecauseItIsTheOnlyOperatorWordUsed() {
        assertEquals(List.of("buy", "sell"), RetrievalService.terms("buy OR sell or"));
    }

    @Test
    void thereAreAtMostTwelveTermsAndNonLatinLettersSurvive() {
        List<String> terms = RetrievalService.terms("a b c d e f g h i j k l m n o p");
        assertEquals(RetrievalService.MAX_TERMS, terms.size());
        assertEquals(List.of("خوف", "السوق"), RetrievalService.terms("خوف السوق"));
    }

    @Test
    void nothingToSearchForGivesNoTerms() {
        assertEquals(List.of(), RetrievalService.terms(null));
        assertEquals(List.of(), RetrievalService.terms("  ?! -- "));
    }

    @Test
    void aShortTextIsReturnedWhole() {
        assertEquals("calm month", RetrievalService.excerpt("  calm month \n", List.of("calm")));
        assertEquals("", RetrievalService.excerpt(null, List.of()));
    }

    @Test
    void aLongTextWithoutAMatchIsCutFromTheStart() {
        String text = "start " + "x".repeat(2000);
        String out = RetrievalService.excerpt(text, List.of("absent"));
        assertTrue(out.startsWith("start "));
        assertTrue(out.endsWith("…"));
        assertFalse(out.startsWith("…"));
        assertTrue(out.length() <= RetrievalService.MAX_SNIPPET_CHARS + 1);
    }

    @Test
    void aLateMatchIsKeptInViewWithLeadingAndTrailingEllipses() {
        String text = ("filler ".repeat(300)) + "then the crash scared me badly " + ("tail ".repeat(300));
        String out = RetrievalService.excerpt(text, List.of("crash"));
        assertTrue(out.contains("crash scared me"), out);
        assertTrue(out.startsWith("…"));
        assertTrue(out.endsWith("…"));
        assertTrue(out.length() <= RetrievalService.MAX_SNIPPET_CHARS + 2);
        assertFalse(out.startsWith("… ") , "starts at a word, not on stray whitespace");
    }

    @Test
    void anEarlyMatchStartsAtTheBeginningWithoutALeadingEllipsis() {
        String text = "the crash came early " + "y".repeat(2000);
        String out = RetrievalService.excerpt(text, List.of("crash"));
        assertTrue(out.startsWith("the crash"));
        assertTrue(out.endsWith("…"));
    }

    @Test
    void veryShortTermsDoNotPullTheWindowAround() {
        String text = "a".repeat(400) + " i went " + "z".repeat(2000) + " panic";
        // "i" is too short to anchor on; "panic" is at the very end.
        String out = RetrievalService.excerpt(text, List.of("i", "panic"));
        assertTrue(out.contains("panic"));
    }
}
