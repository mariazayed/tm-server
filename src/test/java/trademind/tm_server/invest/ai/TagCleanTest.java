package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.ai.TagReply.Item;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagCleanTest {

    private static final Set<String> KNOWN = Set.of("fear", "calm", "decision");

    private static TagReply clean(List<Item> items, List<String> have, int limit) {
        return TagSuggestionsService.clean(new TagReply(items), have, KNOWN, limit);
    }

    private static Item item(String tag) {
        return new Item(tag, "mentions it");
    }

    private static List<String> tags(TagReply r) {
        return r.tags().stream().map(Item::tag).toList();
    }

    @Test
    void tagsAreNormalisedLikeTheJournalDoes() {
        assertEquals(List.of("fear", "new idea"), tags(clean(List.of(item("#Fear"), item("  New   IDEA ")), List.of(), 5)));
    }

    @Test
    void duplicatesAndTagsTheEntryAlreadyHasAreDropped() {
        assertEquals(List.of("calm"), tags(clean(List.of(item("fear"), item("FEAR"), item("calm")), List.of("fear"), 5)));
    }

    @Test
    void atMostTwoTagsTheOwnerDoesNotHaveYetAreKept() {
        assertEquals(List.of("fear", "alpha", "beta"),
                tags(clean(List.of(item("fear"), item("alpha"), item("beta"), item("gamma"), item("calm")), List.of(), 5)).subList(0, 3));
        assertEquals(List.of("fear", "alpha", "beta", "calm"),
                tags(clean(List.of(item("fear"), item("alpha"), item("beta"), item("gamma"), item("calm")), List.of(), 5)));
    }

    @Test
    void theLimitCapsTheCount() {
        assertEquals(List.of("fear", "calm"), tags(clean(List.of(item("fear"), item("calm"), item("decision")), List.of(), 2)));
    }

    @Test
    void unusableTagsAreDropped() {
        List<Item> items = List.of(item(""), item("   "), item("#"), item("buy now!"), item("a".repeat(41)),
                item("a/b"), new Item(null, "x"), item("fear"));
        assertEquals(List.of("fear"), tags(clean(items, List.of(), 5)));
    }

    @Test
    void aSuggestionWhoseReasonIsAdviceIsDropped() {
        TagReply r = clean(List.of(new Item("fear", "you should sell everything"), item("calm")), List.of(), 5);
        assertEquals(List.of("calm"), tags(r));
    }

    @Test
    void aMissingReasonBecomesEmptyAndALongOneIsShortened() {
        TagReply r = clean(List.of(new Item("fear", null), new Item("calm", "x".repeat(300))), List.of(), 5);
        assertEquals("", r.tags().get(0).reason());
        assertEquals(TagSuggestionsService.MAX_REASON_CHARS, r.tags().get(1).reason().length());
    }

    @Test
    void anEmptyListIsAValidAnswer() {
        assertEquals(List.of(), clean(List.of(), List.of(), 5).tags());
    }

    @Test
    void aMissingListAsksForARepeat() {
        assertThrows(ModelJsonException.class, () -> TagSuggestionsService.clean(new TagReply(null), List.of(), KNOWN, 5));
    }
}
