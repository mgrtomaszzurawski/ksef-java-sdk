/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenQueryRequest;

/**
 * Client for KSeF API token management — generate, list, query status, and revoke tokens.
 *
 * @since 1.0.0
 */
public interface Tokens {

    GenerateTokenResult generate(TokenGenerateRequest request);

    /**
     * Single-page list honouring the request's filter parameters
     * ({@code status[]}, {@code description}, {@code authorIdentifier},
     * {@code authorIdentifierType}, {@code pageSize}) and explicit
     * {@code continuationToken} for page navigation. On the first call
     * leave {@link TokenQueryRequest#continuationToken()} {@code null};
     * for subsequent pages feed back the
     * {@link TokenList#continuationToken()} returned by the previous
     * call. The cursor is propagated via the spec's
     * {@code x-continuation-token} request header. For lazy traversal
     * across every page prefer {@link #streamTokens(TokenQueryRequest)}.
     *
     * <p>Pass
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#create()
     * TokenQueryBuilder.create().build()} for an unfiltered first-page
     * query.
     */
    TokenList queryTokens(TokenQueryRequest filter);

    /**
     * Stream every token matching the given filter. Pages are fetched
     * lazily following the {@code x-continuation-token} cursor; the
     * filter parameters are forwarded on every page. Pass
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#create()
     * TokenQueryBuilder.create().build()} for an unfiltered stream.
     *
     * <p>{@link TokenQueryRequest#continuationToken()} is
     * <em>ignored</em> — the SDK paginator always starts from the
     * beginning of the result set. Use {@link #queryTokens} when
     * navigating from a known cursor.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> streamTokens(TokenQueryRequest filter);

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);
}
