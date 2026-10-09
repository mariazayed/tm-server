package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelJsonTest {

    enum Level { LOW, HIGH }

    record Item(long id, String quote) {
    }

    record Report(String title, int count, Level level, List<Item> items, boolean ok) {
    }

    private static final String VALID = """
            {"title":"Check","count":2,"level":"HIGH","items":[{"id":12,"quote":"I was scared"}],"ok":true}""";

    private static final String NOT_JSON = "it was not valid JSON, or had text around it";

    private static String reasonFor(String raw) {
        return assertThrows(ModelJsonException.class, () -> ModelJson.parse(raw, Report.class)).getMessage();
    }

    @Test
    void aKeyThatIsNotInTheShapeIsIgnoredNotEchoed() {
        Report r = ModelJson.parse(VALID.replace("\"items\":[", "\"SECRET-KEY\":[{\"id\":1,\"quote\":\"q\"}],\"items\":["), Report.class);
        assertEquals("Check", r.title());
    }

    @Test
    void aValidReplyParsesIntoTheRecord() {
        Report r = ModelJson.parse(VALID, Report.class);
        assertEquals("Check", r.title());
        assertEquals(2, r.count());
        assertEquals(Level.HIGH, r.level());
        assertEquals(List.of(new Item(12, "I was scared")), r.items());
        assertTrue(r.ok());
    }

    @Test
    void unknownKeysAreIgnoredAndNullIsAllowedForObjects() {
        Report r = ModelJson.parse("""
                {"title":null,"count":0,"level":"LOW","items":[],"ok":false,"extra":{"x":1}}""", Report.class);
        assertNull(r.title());
        assertEquals(List.of(), r.items());
    }

    // ---- code fences and stray text -----------------------------------------------------------

    @Test
    void oneWrappingCodeFenceIsRemoved() {
        for (String wrapped : List.of("```json\n" + VALID + "\n```", "```\n" + VALID + "\n```", "```JSON\n" + VALID + "\n```\n",
                "  \n```json\r\n" + VALID + "\r\n```  ", "﻿" + VALID)) {
            assertEquals("Check", ModelJson.parse(wrapped, Report.class).title(), wrapped);
        }
    }

    @Test
    void textAroundTheJsonIsAnErrorNotSomethingToDigThrough() {
        assertEquals(NOT_JSON, reasonFor("Here is the result:\n" + VALID));
        assertEquals(NOT_JSON, reasonFor("Sure!\n```json\n" + VALID + "\n```"));
        assertEquals(NOT_JSON, reasonFor(VALID + "\nHope that helps!"));
        assertEquals("there was extra text after the JSON object", reasonFor(VALID + VALID));
    }

    @Test
    void anUnclosedFenceIsNotStripped() {
        assertEquals(NOT_JSON, reasonFor("```json\n" + VALID));
    }

    // ---- shape --------------------------------------------------------------------------------

    @Test
    void emptyAndNonJsonRepliesAreRejected() {
        assertEquals("the reply was empty", reasonFor(""));
        assertEquals("the reply was empty", reasonFor("  \n "));
        assertEquals("the reply was empty", reasonFor(null));
        assertEquals(NOT_JSON, reasonFor("not json at all"));
        assertEquals(NOT_JSON, reasonFor("{'single': 'quotes'}"));
        assertEquals("the reply was not a JSON object", reasonFor("null"));
    }

    @Test
    void aReplyThatWasCutOffSaysSo() {
        assertEquals("the JSON was cut off before it ended", reasonFor(VALID.substring(0, 40)));
        assertEquals("the JSON was cut off before it ended", reasonFor("{\"title\":\"Che"));
    }

    @Test
    void theTopLevelMustBeAnObject() {
        assertEquals("the top level must be one JSON object in the requested shape", reasonFor("[" + VALID + "]"));
        assertEquals("the top level must be one JSON object in the requested shape", reasonFor("\"just a string\""));
        assertEquals("the top level must be one JSON object in the requested shape", reasonFor("42"));
    }

    @Test
    void aMissingFieldIsNamed() {
        assertEquals("the field 'level' is missing",
                reasonFor("{\"title\":\"x\",\"count\":1,\"items\":[],\"ok\":true}"));
        assertEquals("the field 'items' is missing", reasonFor("{\"title\":\"x\",\"count\":1,\"level\":\"LOW\",\"ok\":true}"));
    }

    @Test
    void aMissingFieldInsideAListItemIsNamed() {
        String reason = reasonFor("{\"title\":\"x\",\"count\":1,\"level\":\"LOW\",\"ok\":true,\"items\":[{\"id\":1}]}");
        assertEquals("the field 'items[0].quote' is missing", reason);
    }

    @Test
    void wrongTypesAreNamedAndNotCoerced() {
        assertEquals("the field 'count' has the wrong type or a value that is not allowed",
                reasonFor(VALID.replace("\"count\":2", "\"count\":\"2\"")));
        assertEquals("the field 'count' has the wrong type or a value that is not allowed",
                reasonFor(VALID.replace("\"count\":2", "\"count\":2.5")));
        assertEquals("the field 'ok' has the wrong type or a value that is not allowed",
                reasonFor(VALID.replace("\"ok\":true", "\"ok\":\"true\"")));
        assertEquals("the field 'items' has the wrong type or a value that is not allowed",
                reasonFor(VALID.replace("\"items\":[{\"id\":12,\"quote\":\"I was scared\"}]", "\"items\":\"none\"")));
    }

    @Test
    void anEnumTakesOnlyItsExactNames() {
        String bad = "the field 'level' has the wrong type or a value that is not allowed";
        assertEquals(bad, reasonFor(VALID.replace("\"HIGH\"", "\"CRITICAL\"")));
        assertEquals(bad, reasonFor(VALID.replace("\"HIGH\"", "\"high\"")));
        assertEquals(bad, reasonFor(VALID.replace("\"HIGH\"", "1")));
    }

    @Test
    void aPrimitiveCannotBeNull() {
        assertEquals("the field 'count' has the wrong type or a value that is not allowed",
                reasonFor(VALID.replace("\"count\":2", "\"count\":null")));
    }

    @Test
    void aNestedProblemShowsItsPath() {
        String reason = reasonFor(VALID.replace("\"id\":12", "\"id\":\"twelve\""));
        assertEquals("the field 'items[0].id' has the wrong type or a value that is not allowed", reason);
    }

    // ---- the reason never carries the reply ---------------------------------------------------

    @Test
    void noReasonRepeatsWhatTheModelWrote() {
        String secret = "SECRET-MODEL-TEXT-sk-ant-123";
        for (String raw : List.of(
                VALID.replace("\"HIGH\"", "\"" + secret + "\""),
                VALID.replace("\"count\":2", "\"count\":\"" + secret + "\""),
                secret + VALID,
                "{\"" + secret + "\":1}",
                "{\"title\":\"" + secret,
                "[\"" + secret + "\"]",
                VALID.replace("\"quote\":\"I was scared\"", "\"" + secret + "\":5"))) {
            ModelJsonException e = assertThrows(ModelJsonException.class, () -> ModelJson.parse(raw, Report.class));
            assertFalse(e.getMessage().contains("SECRET"), e.getMessage());
            assertFalse(e.getMessage().contains("sk-ant"), e.getMessage());
            assertTrue(e.getMessage().length() <= 200);
            assertFalse(e.getMessage().contains("\n"));
        }
    }

    @Test
    void aCallersReasonIsMadeOneBoundedLine() {
        ModelJsonException e = new ModelJsonException("line one\nline two   " + "x".repeat(500));
        assertFalse(e.getMessage().contains("\n"));
        assertEquals(200, e.getMessage().length());
        assertEquals("the reply was not usable", new ModelJsonException(null).getMessage());
    }
}
