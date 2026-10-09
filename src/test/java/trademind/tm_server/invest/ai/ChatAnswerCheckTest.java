package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAnswerCheckTest {

    private static final Set<String> VALID = Set.of("J12", "J3", "N7");

    private static ChatAnswerCheck.Checked check(String raw) {
        return ChatAnswerCheck.check(raw, VALID);
    }

    // ---- citations ----------------------------------------------------------------------------

    @Test
    void validCitationsBecomeSourcesInOrderOfFirstUse() {
        ChatAnswerCheck.Checked c = check("You felt scared [J12] and later read about it [N7]. Still scared [J12].");
        assertEquals(List.of("J12", "N7"), c.handles());
        assertEquals("You felt scared [J12] and later read about it [N7]. Still scared [J12].", c.answer());
        assertNull(c.refusal());
    }

    @Test
    void aMadeUpCitationIsRemovedFromTheTextAndNeverBecomesASource() {
        ChatAnswerCheck.Checked c = check("You felt scared [J12] and later calm [J999].");
        assertEquals("You felt scared [J12] and later calm.", c.answer());
        assertEquals(List.of("J12"), c.handles());
    }

    @Test
    void aHandleThatWasNotInThisPromptIsInvalidEvenIfItLooksRight() {
        ChatAnswerCheck.Checked c = ChatAnswerCheck.check("Felt calm [J5].", Set.of("J12"));
        assertEquals("Felt calm.", c.answer());
        assertEquals(List.of(), c.handles());
    }

    @Test
    void aBracketListIsSplitAndOnlyTheValidPartsKept() {
        ChatAnswerCheck.Checked c = check("Both agree [J12, N7, J404].");
        assertEquals("Both agree [J12][N7].", c.answer());
        assertEquals(List.of("J12", "N7"), c.handles());
    }

    @Test
    void textThatLooksLikeACitationButIsNotOneIsLeftAlone() {
        ChatAnswerCheck.Checked c = check("See [note 7] and [journal#12] and [J] and [12].");
        assertEquals("See [note 7] and [journal#12] and [J] and [12].", c.answer());
        assertEquals(List.of(), c.handles());
    }

    @Test
    void anAnswerThatWasOnlyBadCitationsCountsAsEmpty() {
        ChatAnswerCheck.Checked c = check("[J999]");
        assertEquals(ChatAnswerCheck.EMPTY, c.refusal());
        assertEquals(ChatAnswerCheck.EMPTY_REPLY, c.answer());
    }

    @Test
    void blankOrNullRepliesAreEmpty() {
        assertEquals(ChatAnswerCheck.EMPTY, check("   \n").refusal());
        assertEquals(ChatAnswerCheck.EMPTY, check(null).refusal());
    }

    // ---- advice -------------------------------------------------------------------------------

    @Test
    void adviceIsReplacedByTheFixedRefusalAndDropsTheSources() {
        for (String reply : List.of(
                "You should sell now [J12].",
                "Given the dip, you must buy more.",
                "You might want to hold through this.",
                "You could consider pausing your plan.",
                "You'd better reduce the monthly amount.",
                "I recommend buying more VT.",
                "I'd suggest you sell before it falls.",
                "This looks like a good time to buy.",
                "VT will reach 150 by March.",
                "The price is going to drop below 90 soon.",
                "You can expect returns of about 8% a year.",
                "You felt calm [J12]. But honestly you should sell.")) {
            ChatAnswerCheck.Checked c = check(reply);
            assertEquals(ChatAnswerCheck.ADVICE, c.refusal(), reply);
            assertEquals(ChatAnswerCheck.ADVICE_REFUSAL, c.answer(), reply);
            assertEquals(List.of(), c.handles(), reply);
        }
    }

    @Test
    void restatingTheOwnersWordsIsNotAdvice() {
        for (String reply : List.of(
                "Your rule says you should never sell in a crash [J12].",
                "You wrote that you wanted to sell, then waited 72 hours [J12].",
                "In March you noted that you should not buy extra after a dip [J3].",
                "The rules you wrote say you should hold your plan steady.",
                "You should wait 72 hours before any deviation.",
                "In March you felt anxious [J3] and still bought on the usual day.",
                "The plan buys $300 on day 1 of each month.",
                "You have bought 7 months in a row.")) {
            ChatAnswerCheck.Checked c = check(reply);
            assertNull(c.refusal(), reply);
            assertFalse(c.answer().isBlank(), reply);
        }
    }

    @Test
    void adviceOnAnyLineOfAMultiLineAnswerIsCaught() {
        assertEquals(ChatAnswerCheck.ADVICE, check("Here is what you wrote:\n- calm [J12]\n\nSo you should buy more.").refusal());
    }

    @Test
    void anInventedCitationCannotHideAdviceByStandingInTheWay() {
        // The citation is removed first, so the sentence is judged as the reader will see it.
        assertEquals(ChatAnswerCheck.ADVICE, check("You should [J999] sell now.").refusal());
    }
}
