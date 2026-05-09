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
public interface TokenClient {

    GenerateTokenResult generate(TokenGenerateRequest request);

    /**
     * Single-page list with the five spec-defined filter parameters
     * ({@code status[]}, {@code description}, {@code authorIdentifier},
     * {@code authorIdentifierType}, {@code pageSize}). The continuation
     * token is preserved on the returned {@link TokenList} for callers
     * who want to drive paging manually; for lazy paging prefer
     * {@link #streamTokens(TokenQueryRequest)}.
     *
     * <p>Pass
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#create()
     * TokenQueryBuilder.create().build()} for an unfiltered query.
     */
    TokenList list(TokenQueryRequest filter);

    /**
     * Stream every token matching the given filter. Pages are fetched
     * lazily following the {@code x-continuation-token} cursor; the
     * filter parameters are forwarded on every page. Pass
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder#create()
     * TokenQueryBuilder.create().build()} for an unfiltered stream.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> streamTokens(TokenQueryRequest filter);

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);
}
