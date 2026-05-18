/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code Tokens.queryTokens(...)} and
 * {@code Tokens.streamTokens(...)}. Captures the filter parameters
 * defined by the spec for {@code GET /tokens} plus the
 * {@code x-continuation-token} cursor that drives explicit page
 * navigation. All filter fields are optional; a record with no fields
 * populated matches every token visible to the authenticated principal.
 *
 * <p>Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#build()}.
 *
 * <p><strong>Pagination model</strong>: KSeF's {@code GET /tokens}
 * endpoint paginates via a continuation token (header
 * {@code x-continuation-token}) rather than offset/limit. To navigate
 * pages through {@code queryTokens} the consumer feeds the
 * {@link TokenList#continuationToken() continuationToken} from one
 * page back into {@link #continuationToken()} for the next request.
 * {@code streamTokens} ignores any caller-supplied cursor and starts a
 * fresh walk from the beginning of the result set.
 *
 * @param continuationToken cursor returned by the previous
 *     {@code queryTokens} call's
 *     {@link TokenList#continuationToken()}; {@code null} on the first
 *     call (start of the result set). Ignored by {@code streamTokens}.
 * @param pageSize server-bounded page size (range 10-100 per OpenAPI);
 *     {@code null} defers to the server default (10).
 *
 * @since 1.0.0
 */
public record TokenQueryRequest(
        List<TokenStatus> statuses,
        @Nullable String description,
        @Nullable String authorIdentifier,
        @Nullable TokenAuthorIdentifierType authorIdentifierType,
        @Nullable String continuationToken,
        @Nullable Integer pageSize) {

    public TokenQueryRequest {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
}
