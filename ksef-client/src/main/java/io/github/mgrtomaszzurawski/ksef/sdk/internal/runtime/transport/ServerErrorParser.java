/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefValidationError;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Best-effort parser for the two KSeF wire shapes of a 400 response body:
 *
 * <ol>
 *   <li><b>RFC 7807 Problem Details</b> (when consumer opts in via
 *       {@code FeaturePolicy.problemDetails(true)}):
 *       <pre>{@code
 *       {"title":"...","status":400,"errors":[{"code":21405,
 *         "description":"...","details":["..."]}]}
 *       }</pre>
 *   </li>
 *   <li><b>Legacy {@code application/json}</b>:
 *       <pre>{@code
 *       {"exception":{"exceptionDetailList":[{"exceptionCode":21405,
 *         "exceptionDescription":"...","details":["..."]}]}}
 *       }</pre>
 *   </li>
 * </ol>
 *
 * <p>Returns an empty list when the body is null, blank, malformed, or
 * does not match either shape — callers should treat the empty list as
 * "no structured detail available" and fall back to {@code responseBody}.
 *
 * <p>Internal — never call from consumer code.
 *
 * @since 1.0.0
 */
public final class ServerErrorParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerErrorParser.class);
    private static final String LOG_MALFORMED_BODY =
            "ServerErrorParser: 400 response body is not valid JSON — returning empty errors list";
    private static final String LOG_NO_ENVELOPE_MATCHED =
            "ServerErrorParser: 400 response body parsed but matched neither RFC 7807 Problem"
            + " Details nor legacy exception envelope — wire format may have drifted (body length: {})";

    /**
     * Reuse a single Jackson {@link ObjectMapper}. Uses defaults
     * intentionally: this parser only navigates {@link JsonNode} paths
     * by literal name and reads scalar values via
     * {@link JsonNode#asInt()} / {@link JsonNode#asText()} — it never
     * binds a Java type, so {@code JavaTimeModule}/{@code JsonNullableModule}
     * configuration on the runtime mapper is not relevant here.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Problem Details — top-level {@code errors[]} array. */
    private static final String PROBLEM_DETAILS_ERRORS = "errors";
    private static final String PROBLEM_DETAILS_CODE = "code";
    private static final String PROBLEM_DETAILS_DESCRIPTION = "description";

    /** Legacy — {@code exception.exceptionDetailList[]}. */
    private static final String LEGACY_EXCEPTION = "exception";
    private static final String LEGACY_EXCEPTION_DETAIL_LIST = "exceptionDetailList";
    private static final String LEGACY_EXCEPTION_CODE = "exceptionCode";
    private static final String LEGACY_EXCEPTION_DESCRIPTION = "exceptionDescription";

    /** Inner detail-string array name — identical in both wire shapes. */
    private static final String FIELD_DETAILS = "details";

    private ServerErrorParser() { }

    /**
     * Parse {@code body} into the normalised
     * {@link KsefValidationError} list. Tries Problem Details first,
     * then the legacy shape. Returns empty when neither matches.
     */
    public static List<KsefValidationError> parseErrors(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (JsonProcessingException malformed) {
            LOGGER.debug(LOG_MALFORMED_BODY, malformed);
            return List.of();
        }
        if (root == null || root.isMissingNode() || root.isNull()) {
            return List.of();
        }
        List<KsefValidationError> problemDetails = parseProblemDetails(root);
        if (!problemDetails.isEmpty()) {
            return problemDetails;
        }
        List<KsefValidationError> legacy = parseLegacy(root);
        if (legacy.isEmpty()) {
            // Body parsed cleanly but neither documented envelope matched.
            // Helpful breadcrumb when KSeF wire format drifts post-1.0 —
            // operator otherwise just sees an empty errors() list with no
            // signal to investigate.
            LOGGER.debug(LOG_NO_ENVELOPE_MATCHED, body.length());
        }
        return legacy;
    }

    /**
     * Convenience — extract the first error's {@code code} (if any).
     * Returns {@code null} when the body is null/blank, malformed JSON,
     * or carries no recognised structured error envelope (neither
     * Problem Details nor the legacy shape).
     */
    public static @Nullable Integer firstExceptionCode(@Nullable String body) {
        List<KsefValidationError> errors = parseErrors(body);
        return errors.isEmpty() ? null : errors.get(0).code();
    }

    private static List<KsefValidationError> parseProblemDetails(JsonNode root) {
        JsonNode errorsNode = root.get(PROBLEM_DETAILS_ERRORS);
        if (errorsNode == null || !errorsNode.isArray()) {
            return List.of();
        }
        List<KsefValidationError> errors = new ArrayList<>(errorsNode.size());
        for (JsonNode item : errorsNode) {
            KsefValidationError parsed = readError(item,
                    PROBLEM_DETAILS_CODE, PROBLEM_DETAILS_DESCRIPTION, FIELD_DETAILS);
            if (parsed != null) {
                errors.add(parsed);
            }
        }
        return List.copyOf(errors);
    }

    private static List<KsefValidationError> parseLegacy(JsonNode root) {
        JsonNode exceptionNode = root.get(LEGACY_EXCEPTION);
        if (exceptionNode == null || !exceptionNode.isObject()) {
            return List.of();
        }
        JsonNode listNode = exceptionNode.get(LEGACY_EXCEPTION_DETAIL_LIST);
        if (listNode == null || !listNode.isArray()) {
            return List.of();
        }
        List<KsefValidationError> errors = new ArrayList<>(listNode.size());
        for (JsonNode item : listNode) {
            KsefValidationError parsed = readError(item,
                    LEGACY_EXCEPTION_CODE, LEGACY_EXCEPTION_DESCRIPTION, FIELD_DETAILS);
            if (parsed != null) {
                errors.add(parsed);
            }
        }
        return List.copyOf(errors);
    }

    private static @Nullable KsefValidationError readError(JsonNode item,
                                                           String codeField,
                                                           String descriptionField,
                                                           String detailsField) {
        JsonNode codeNode = item.get(codeField);
        JsonNode descNode = item.get(descriptionField);
        if (codeNode == null || !codeNode.canConvertToInt() || descNode == null) {
            return null;
        }
        // Legacy `ExceptionDetails.exceptionDescription` is marked nullable
        // in the OpenAPI spec. Jackson returns NullNode for explicit JSON
        // null, whose `asText()` would otherwise yield the literal string
        // "null". Substitute "" so consumers see an empty description
        // instead of misleading text — preserves the error code (the
        // operationally useful field) without inventing wording.
        String description = descNode.isNull() ? "" : descNode.asText();
        List<String> details = readDetails(item.get(detailsField));
        return new KsefValidationError(codeNode.asInt(), description, details);
    }

    private static List<String> readDetails(@Nullable JsonNode detailsNode) {
        if (detailsNode == null || !detailsNode.isArray() || detailsNode.isEmpty()) {
            return List.of();
        }
        List<String> details = new ArrayList<>(detailsNode.size());
        for (JsonNode detail : detailsNode) {
            if (detail != null && !detail.isNull()) {
                details.add(detail.asText());
            }
        }
        return List.copyOf(details);
    }
}
