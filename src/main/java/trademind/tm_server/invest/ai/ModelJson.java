package trademind.tm_server.invest.ai;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a model reply into a typed record, or says precisely why it cannot. This is the only way the
 * JSON-producing AI features read a reply, so nothing unparsed or unchecked reaches the client.
 *
 * <p>Rules:
 * <ul>
 *   <li>The whole reply must be one JSON object. A single wrapping code fence ({@code ```json ... ```})
 *       is removed, because models add one even when told not to; any other text before or after is an
 *       error, not something to dig the JSON out of.
 *   <li>Every record component must be present as a key. {@code null} is accepted for object fields (a
 *       caller's own check can reject it) but not for primitives. Unknown keys are ignored.
 *   <li>Types are strict: the string {@code "5"} is not the number 5, 5.5 is not an int, and an enum accepts
 *       only its exact names (not a number, not another case).
 *   <li>A failure's message names the problem and the field, never the value the model wrote.
 * </ul>
 */
public final class ModelJson {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION) // keeps reply text out of exception messages
            .build();

    private static final Pattern FENCE = Pattern.compile("\\A```[A-Za-z]*[ \\t]*\\R(.*?)\\R?[ \\t]*```\\z", Pattern.DOTALL);
    private static final Pattern MISSING = Pattern.compile("Missing (?:required )?creator property '([^']+)'");
    private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z0-9_.\\[\\]]{1,60}");

    /** Invalid JSON and prose around valid JSON look the same to the parser, so one reason covers both. */
    static final String NOT_JSON = "it was not valid JSON, or had text around it";

    private ModelJson() {
    }

    /** @throws ModelJsonException with a safe reason when {@code raw} is not a valid {@code type} */
    public static <T> T parse(String raw, Class<T> type) {
        String json = unwrap(raw);
        if (json.isEmpty()) throw new ModelJsonException("the reply was empty");
        try {
            T value = MAPPER.readValue(json, type);
            if (value == null) throw new ModelJsonException("the reply was not a JSON object");
            return value;
        } catch (JsonParseException e) {
            throw new ModelJsonException(syntaxProblem(e.getOriginalMessage()));
        } catch (MismatchedInputException e) {
            throw new ModelJsonException(describe(e));
        } catch (JsonMappingException e) {
            // A syntax error met while binding a record's fields comes wrapped in a mapping exception.
            boolean syntax = e.getCause() instanceof JsonParseException
                    || String.valueOf(e.getOriginalMessage()).contains("end-of-input");
            throw new ModelJsonException(syntax ? syntaxProblem(e.getOriginalMessage()) : fieldProblem(e, "has the wrong type or a value that is not allowed"));
        } catch (java.io.IOException e) {
            throw new ModelJsonException(NOT_JSON);
        }
    }

    /** Trims, drops a byte-order mark, and removes one code fence that wraps the whole reply. */
    static String unwrap(String raw) {
        if (raw == null) return "";
        String s = raw.replace("﻿", "").strip();
        Matcher m = FENCE.matcher(s);
        return m.matches() ? m.group(1).strip() : s;
    }

    private static String syntaxProblem(String originalMessage) {
        String message = String.valueOf(originalMessage);
        boolean endedEarly = message.contains("end-of-input") || message.contains("Unexpected end");
        return endedEarly ? "the JSON was cut off before it ended" : NOT_JSON;
    }

    private static String describe(MismatchedInputException e) {
        String message = String.valueOf(e.getOriginalMessage());
        if (MISSING.matcher(message).find()) return fieldProblem(e, "is missing");
        if (message.contains("Trailing token")) return "there was extra text after the JSON object";
        if (e.getPath().isEmpty()) return "the top level must be one JSON object in the requested shape";
        return fieldProblem(e, "has the wrong type or a value that is not allowed");
    }

    private static String fieldProblem(JsonMappingException e, String problem) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                if (!path.isEmpty()) path.append('.');
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        if (path.isEmpty()) return "the JSON did not match the requested shape";
        return "the field '" + safe(path.toString()) + "' " + problem;
    }

    /** Field names come from our own records, but a path is cheap to constrain. */
    private static String safe(String path) {
        return SAFE_PATH.matcher(path).matches() ? path : "?";
    }
}
