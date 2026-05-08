/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefValidationError;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct unit coverage for {@link ServerErrorParser} branches that the
 * indirect path through {@code KsefException.of(...)} does not exercise:
 * null/blank/malformed bodies, half-shape envelopes, missing fields,
 * wrong-type fields, and the Problem-Details-wins-over-legacy precedence
 * when both shapes appear in the same body.
 */
class ServerErrorParserTest {

    private static final int CODE_FIELD_VALIDATION = 21405;
    private static final int CODE_JSON_PARSE = 21001;
    private static final int CODE_BATCH_EMPTY = 21205;
    private static final String DETAIL_FIRST = "first detail";
    private static final String DETAIL_SECOND = "second detail";

    @Test
    void parseErrors_whenBodyNull_returnsEmptyList() {
        assertTrue(ServerErrorParser.parseErrors(null).isEmpty());
    }

    @Test
    void parseErrors_whenBodyBlank_returnsEmptyList() {
        assertTrue(ServerErrorParser.parseErrors("   ").isEmpty());
    }

    @Test
    void parseErrors_whenBodyMalformedJson_returnsEmptyList() {
        assertTrue(ServerErrorParser.parseErrors("not<json>at all").isEmpty());
    }

    @Test
    void parseErrors_whenProblemDetailsErrorsFieldNotArray_returnsEmptyList() {
        // Problem Details path: errors must be an array — string here.
        String body = """
                {"errors":"oops","status":400}
                """;
        assertTrue(ServerErrorParser.parseErrors(body).isEmpty());
    }

    @Test
    void parseErrors_whenProblemDetailsItemMissingCode_skipsItem() {
        // First item missing code → skipped; second valid → kept.
        String body = """
                {
                  "errors": [
                    {"description":"no code"},
                    {"code":21405,"description":"Invalid field"}
                  ]
                }
                """;
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(1, errors.size());
        assertEquals(CODE_FIELD_VALIDATION, errors.get(0).code());
    }

    @Test
    void parseErrors_whenProblemDetailsCodeNotNumeric_skipsItem() {
        String body = """
                {"errors":[{"code":"twenty-one-thousand","description":"oops"}]}
                """;
        assertTrue(ServerErrorParser.parseErrors(body).isEmpty());
    }

    @Test
    void parseErrors_whenLegacyExceptionFieldNotObject_returnsEmptyList() {
        String body = """
                {"exception":"oops"}
                """;
        assertTrue(ServerErrorParser.parseErrors(body).isEmpty());
    }

    @Test
    void parseErrors_whenLegacyDetailListNotArray_returnsEmptyList() {
        String body = """
                {"exception":{"exceptionDetailList":"oops"}}
                """;
        assertTrue(ServerErrorParser.parseErrors(body).isEmpty());
    }

    @Test
    void parseErrors_whenLegacyDescriptionNull_preservesItemWithEmptyDescription() {
        // OpenAPI marks ExceptionDetails.exceptionDescription as nullable.
        // The parser preserves the item (the error code is operationally
        // valuable) but substitutes "" for the description — Jackson's
        // NullNode.asText() would otherwise yield the literal string "null"
        // which is misleading when shown to users.
        String body = """
                {
                  "exception": {
                    "exceptionDetailList": [
                      {"exceptionCode":21001,"exceptionDescription":null},
                      {"exceptionCode":21405,"exceptionDescription":"valid description"}
                    ]
                  }
                }
                """;
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(2, errors.size());
        assertEquals(CODE_JSON_PARSE, errors.get(0).code());
        assertEquals("", errors.get(0).description());
        assertEquals(CODE_FIELD_VALIDATION, errors.get(1).code());
        assertEquals("valid description", errors.get(1).description());
    }

    @Test
    void parseErrors_whenBothShapesPresent_problemDetailsWins() {
        // Precedence pin: Problem Details errors[] takes precedence over
        // the legacy envelope when both happen to be present.
        String body = """
                {
                  "errors": [{"code":21205,"description":"batch empty"}],
                  "exception": {
                    "exceptionDetailList": [
                      {"exceptionCode":21001,"exceptionDescription":"json parse"}
                    ]
                  }
                }
                """;
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(1, errors.size());
        assertEquals(CODE_BATCH_EMPTY, errors.get(0).code());
    }

    @Test
    void readDetails_whenEmptyArray_returnsEmptyList() {
        // details is an array but empty → no details surfaced.
        String body = """
                {"errors":[{"code":21405,"description":"Invalid field","details":[]}]}
                """;
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).details().isEmpty());
    }

    @Test
    void readDetails_whenContainsNullElement_filtersIt() {
        String body = """
                {"errors":[{"code":21405,"description":"Invalid field","details":["%s",null,"%s"]}]}
                """.formatted(DETAIL_FIRST, DETAIL_SECOND);
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(2, errors.get(0).details().size());
        assertEquals(DETAIL_FIRST, errors.get(0).details().get(0));
        assertEquals(DETAIL_SECOND, errors.get(0).details().get(1));
    }

    @Test
    void readDetails_whenNotArray_returnsEmptyList() {
        // details is a string instead of array → ignored.
        String body = """
                {"errors":[{"code":21405,"description":"Invalid field","details":"not an array"}]}
                """;
        List<KsefValidationError> errors = ServerErrorParser.parseErrors(body);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).details().isEmpty());
    }

    @Test
    void firstExceptionCode_whenMultipleErrors_returnsFirst() {
        // Pins documented "first error" semantics — different codes per item
        // so a regression that returned a different element would surface.
        String body = """
                {
                  "errors": [
                    {"code":21001,"description":"json parse"},
                    {"code":21405,"description":"field invalid"}
                  ]
                }
                """;
        Integer firstCode = ServerErrorParser.firstExceptionCode(body);
        assertEquals(Integer.valueOf(CODE_JSON_PARSE), firstCode);
    }

    @Test
    void firstExceptionCode_whenBodyNull_returnsNull() {
        assertNull(ServerErrorParser.firstExceptionCode(null));
    }

    @Test
    void firstExceptionCode_whenBodyParsesButNoErrorEnvelope_returnsNull() {
        // Body is valid JSON, neither Problem Details nor legacy shape.
        assertNull(ServerErrorParser.firstExceptionCode("{\"foo\":\"bar\"}"));
    }
}
