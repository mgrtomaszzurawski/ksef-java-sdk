/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code Tokens.list(...)} and
 * {@code Tokens.streamTokens(...)}. Captures the filter
 * parameters defined by the spec for {@code GET /tokens}. All
 * fields optional; a record with no fields populated matches every
 * token visible to the authenticated principal.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#build()}.
 *
 * @since 1.0.0
 */
public record TokenQueryRequest(
        List<TokenStatus> statuses,
        @Nullable String description,
        @Nullable String authorIdentifier,
        @Nullable TokenAuthorIdentifierType authorIdentifierType,
        @Nullable Integer pageSize) {

    public TokenQueryRequest {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
}
