/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Single validation error returned by the KSeF server in a 400 response.
 *
 * <p>The KSeF wire format ships in two shapes:
 * <ul>
 *   <li>RFC 7807 Problem Details ({@code application/problem+json}) — top-level
 *       {@code errors[]} array of objects with {@code code} +
 *       {@code description} + {@code details[]}. Used when the consumer
 *       opts in via {@code FeaturePolicy.problemDetails(true)}.</li>
 *   <li>Legacy {@code application/json} — top-level
 *       {@code exception.exceptionDetailList[]} array with the same fields
 *       under {@code exceptionCode} / {@code exceptionDescription}.</li>
 * </ul>
 *
 * <p>This record normalises both shapes into a single representation.
 *
 * @param code KSeF-internal error code (e.g. 21405 per-field validation,
 *     21001 JSON parsing, 21205 batch empty)
 * @param description human-readable description from the server (Polish)
 * @param details optional per-instance detail messages (may be empty);
 *     defensively copied into an immutable {@link List#copyOf(java.util.Collection)}
 *
 * @since 1.0.0
 */
public record KsefValidationError(int code, String description, List<String> details) {

    private static final String ERR_NULL_DESCRIPTION = "description must not be null";
    private static final String ERR_NULL_DETAILS = "details must not be null";

    public KsefValidationError {
        Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
        Objects.requireNonNull(details, ERR_NULL_DETAILS);
        details = List.copyOf(details);
    }

    /** Convenience factory for the common single-detail case. */
    public static KsefValidationError of(int code, String description, @Nullable String detail) {
        return new KsefValidationError(code, description,
                detail == null ? List.of() : List.of(detail));
    }
}
