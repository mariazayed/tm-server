package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberCheckTest {

    private static final List<String> FACTS = List.of(
            "Plan: 300.00 USD per month, buy day 1 (due 2026-09-01).",
            "Bought: 2026-09-05, 300.00 USD for 2.55489022 shares at 117.4250 (fee 0.00 USD).",
            "Timing: first buy 4 days after the buy day.",
            "Value at 2026-09-30: invested to date 900.00 USD, worth 962.15 USD, gain +62.15 USD (+6.91%).",
            "Journal: 4 entries; moods 2, 3, 4, 4 (average 3.3).");

    private static List<String> unknown(String text) {
        return NumberCheck.unknown(text, FACTS);
    }

    @Test
    void numbersThatAreInTheFactsPassWhateverTheirFormatting() {
        assertEquals(List.of(), unknown("Bought 300 USD for 2.55489022 shares at 117.425."));
        assertEquals(List.of(), unknown("The 300.0 buy came 4 days after the 1st."));
        assertEquals(List.of(), unknown("Worth 962.15 and up 62.15, or 6.91%."));
        assertEquals(List.of(), unknown("Mood averaged 3.3 over 4 entries."));
    }

    @Test
    void datesPassBecauseTheirPartsAreInTheFacts() {
        assertEquals(List.of(), unknown("On 2026-09-05 (the 5th) it was done."));
        assertEquals(List.of(), unknown("September 5, 2026."));
    }

    @Test
    void aNumberThatIsNotInTheFactsIsReportedAsWritten() {
        assertEquals(List.of("10.7"), unknown("Up 10.7% this month."));
        assertEquals(List.of("350"), unknown("Bought 350 USD."));
        assertEquals(List.of("12", "7"), unknown("12 buys and 7 entries, 300 USD."));
    }

    @Test
    void aRoundedFigureDoesNotMatchTheExactOne() {
        assertEquals(List.of("6.9"), unknown("Up 6.9%."));
        assertEquals(List.of("117.43"), unknown("At 117.43."));
    }

    @Test
    void thousandsSeparatorsAreUnderstood() {
        assertEquals(List.of(), NumberCheck.unknown("Worth 1,234.50 USD.", List.of("worth 1234.50 USD")));
        assertEquals(List.of("1,234.60"), NumberCheck.unknown("Worth 1,234.60 USD.", List.of("worth 1234.50 USD")));
        assertEquals(List.of(), NumberCheck.unknown("Moods 2, 3, 4.", List.of("moods 2, 3, 4")), "a comma between numbers is a list");
    }

    @Test
    void repeatsAreReportedOnce() {
        assertEquals(List.of("99"), unknown("99 and 99 and 99."));
    }

    @Test
    void zeroIsZeroInAnyForm() {
        assertEquals(List.of(), NumberCheck.unknown("No fees: 0.", List.of("fee 0.00 USD")));
        assertEquals(List.of("0.5"), NumberCheck.unknown("0.5", List.of("fee 0.00 USD")));
    }

    @Test
    void textWithoutNumbersIsFine() {
        assertEquals(List.of(), unknown("**What happened**\nThe buy was made a few days late."));
        assertEquals(List.of(), NumberCheck.unknown("", List.of()));
    }

    @Test
    void numbersInTheOwnersWordsAreAllowedWhenThoseWordsAreASource() {
        List<String> withEntry = List.of("Plan: 300.00 USD.", "I waited 72 hours before acting.");
        assertEquals(List.of(), NumberCheck.unknown("The owner waited 72 hours.", withEntry));
        assertEquals(List.of("72"), NumberCheck.unknown("The owner waited 72 hours.", List.of("Plan: 300.00 USD.")));
    }
}
