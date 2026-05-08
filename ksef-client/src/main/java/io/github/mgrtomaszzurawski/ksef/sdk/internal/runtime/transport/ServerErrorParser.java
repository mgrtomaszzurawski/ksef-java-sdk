/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefValidationError;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.Nullable;

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

    /**
     * Reuse a single configured Jackson mapper — the parser is invoked
     * from the HTTP error path which is already on a hot path during
     * any 400 response.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Problem Details — top-level {@code errors[]} array. */
    private static final String PROBLEM_DETAILS_ERRORS = "errors";
    private static final String PROBLEM_DETAILS_CODE = "code";
    private static final String PROBLEM_DETAILS_DESCRIPTION = "description";
    private static final String PROBLEM_DETAILS_DETAILS = "details";

    /** Legacy — {@code exception.exceptionDetailList[]}. */
    private static final String LEGACY_EXCEPTION = "exception";
    private static final String LEGACY_EXCEPTION_DETAIL_LIST = "exceptionDetailList";
    private static final String LEGACY_EXCEPTION_CODE = "exceptionCode";
    private static final String LEGACY_EXCEPTION_DESCRIPTION = "exceptionDescription";

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
        } catch (com.fasterxml.jackson.core.JsonProcessingException malformed) {
            return List.of();
        }
        if (root == null || root.isMissingNode() || root.isNull()) {
            return List.of();
        }
        List<KsefValidationError> problemDetails = parseProblemDetails(root);
        if (!problemDetails.isEmpty()) {
            return problemDetails;
        }
        return parseLegacy(root);
    }

    /**
     * Convenience — extract the first error's {@code code} (if any).
     * Returns {@code null} when the body could not be parsed.
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
                    PROBLEM_DETAILS_CODE, PROBLEM_DETAILS_DESCRIPTION, PROBLEM_DETAILS_DETAILS);
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
                    LEGACY_EXCEPTION_CODE, LEGACY_EXCEPTION_DESCRIPTION, PROBLEM_DETAILS_DETAILS);
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
        List<String> details = readDetails(item.get(detailsField));
        return new KsefValidationError(codeNode.asInt(), descNode.asText(), details);
    }

    private static List<String> readDetails(@Nullable JsonNode detailsNode) {
        if (detailsNode == null || !detailsNode.isArray() || detailsNode.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(detailsNode.size());
        for (Iterator<JsonNode> it = detailsNode.elements(); it.hasNext(); ) {
            JsonNode el = it.next();
            if (el != null && !el.isNull()) {
                out.add(el.asText());
            }
        }
        return List.copyOf(out);
    }
}
