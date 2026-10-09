package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteAndIdCheckTest {

    private static final List<String> SOURCES = List.of(
            "I was scared when the market dropped.\n\nBought anyway, on the usual day.",
            "The “crash” felt worse than it was — I wrote this down.");

    private static boolean ok(String quote) {
        return QuoteCheck.verified(quote, SOURCES).isPresent();
    }

    // ---- quotes -------------------------------------------------------------------------------

    @Test
    void anExactPhraseIsVerifiedAndReturnedTrimmed() {
        assertEquals(Optional.of("I was scared"), QuoteCheck.verified("  I was scared ", SOURCES));
        assertTrue(ok("Bought anyway, on the usual day."));
    }

    @Test
    void caseWhitespaceAndTypographyDoNotMatter() {
        assertTrue(ok("i WAS   scared when\nthe market dropped"));
        assertTrue(ok("The \"crash\" felt worse than it was - I wrote this down"));
        assertTrue(ok("the “crash” felt worse"));
    }

    @Test
    void differentWordsAreNotVerified() {
        assertFalse(ok("I was terrified when the market dropped"));
        assertFalse(ok("I was scared and sold everything"));
        assertFalse(ok("market the dropped"));
    }

    @Test
    void tooShortOrTooLongOrEmptyIsNeverVerified() {
        assertFalse(ok("I"));
        assertFalse(ok("the"));
        assertFalse(ok(""));
        assertFalse(ok("   "));
        assertFalse(QuoteCheck.verified(null, SOURCES).isPresent());
        assertFalse(QuoteCheck.verified("I was scared", null).isPresent());
        assertFalse(QuoteCheck.verified("x".repeat(QuoteCheck.MAX_CHARS + 1), List.of("x".repeat(1000))).isPresent());
        assertTrue(QuoteCheck.verified("x".repeat(QuoteCheck.MAX_CHARS), List.of("x".repeat(1000))).isPresent());
    }

    @Test
    void anEllipsisMayLeaveThingsOutButEachPieceMustBeThereInOrder() {
        assertTrue(ok("I was scared ... on the usual day"));
        assertTrue(ok("I was scared … usual day."));
        assertFalse(ok("on the usual day ... I was scared"), "pieces must keep the original order");
        assertFalse(ok("I was scared ... and then sold it all"));
        assertFalse(ok("... ..."), "nothing left to verify");
        assertFalse(ok("I was scared ... a"), "a piece that is too short proves nothing");
    }

    @Test
    void theQuoteMustBeInOneSourceNotSpreadAcrossTwo() {
        assertFalse(ok("I was scared ... I wrote this down"));
        assertTrue(ok("I was scared ... usual day"));
    }

    @Test
    void aSourceThatIsNullIsIgnored() {
        assertTrue(QuoteCheck.verified("I was scared", Arrays.asList(null, SOURCES.get(0))).isPresent());
    }

    // ---- ids ----------------------------------------------------------------------------------

    @Test
    void onlyIdsThatWereShownSurviveInTheModelsOrderWithoutRepeats() {
        Set<Long> shown = Set.of(12L, 7L, 3L);
        assertEquals(List.of(7L, 12L), KnownIds.keep(List.of(7L, 999L, 12L, 7L, 0L), shown));
        assertEquals(List.of(), KnownIds.keep(List.of(1L, 2L), shown));
        assertEquals(List.of(), KnownIds.keep(null, shown));
        assertEquals(List.of(3L), KnownIds.keep(Arrays.asList(null, 3L), shown));
    }

    @Test
    void aSingleIdIsKnownOnlyIfShown() {
        assertTrue(KnownIds.isKnown(12L, Set.of(12L)));
        assertFalse(KnownIds.isKnown(13L, Set.of(12L)));
        assertFalse(KnownIds.isKnown(null, Set.of(12L)));
    }
}
