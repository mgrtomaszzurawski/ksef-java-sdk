/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;

/**
 * Client for KSeF API token management — generate, list, query status, and revoke tokens.
 *
 * @since 1.0.0
 */
public interface TokenClient {

    GenerateTokenResult generate(TokenGenerateBuilder tokenBuilder);

    /**
     * Single-page list with the five spec-defined filter parameters
     * ({@code status[]}, {@code description}, {@code authorIdentifier},
     * {@code authorIdentifierType}, {@code pageSize}). The continuation
     * token is preserved on the returned {@link TokenList} for callers
     * who want to drive paging manually; for lazy paging prefer
     * {@link #streamTokens(TokenQueryBuilder)}.
     *
     * <p>Pass {@link TokenQueryBuilder#create()} for an unfiltered query.
     */
    TokenList list(TokenQueryBuilder filter);

    /**
     * Stream every token matching the given filter. Pages are fetched
     * lazily following the {@code x-continuation-token} cursor; the
     * filter parameters are forwarded on every page. Pass
     * {@link TokenQueryBuilder#create()} for an unfiltered stream.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> streamTokens(TokenQueryBuilder filter);

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);
}
