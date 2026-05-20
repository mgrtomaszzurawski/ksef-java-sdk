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
 * Client for KSeF API token management — generate authentication
 * tokens, list/query existing tokens, poll status, and revoke tokens.
 * Reached via {@code KsefClient.tokens()}.
 *
 * <p><strong>Generation lifecycle</strong>: per
 * {@code ksef-docs/tokeny-ksef.md}, {@link #generate} returns
 * immediately with a reference number and the token value (the value
 * is returned only once). The token starts in {@code Pending} status
 * and transitions to {@code Active} asynchronously after KSeF
 * activates the requested permissions. Consumers either poll
 * {@link #getStatus(String)} until terminal, or trust the lazy-activation
 * pattern and use the token when needed.
 *
 * <p><strong>Pagination model</strong>: cursor-based via
 * {@code x-continuation-token} header (not offset/limit). Pass the
 * {@link TokenList#continuationToken()} from one response back into
 * {@link TokenQueryRequest#continuationToken()} for the next page.
 * Use {@link #streamTokens} for lazy full traversal that handles the
 * cursor internally.
 *
 * @since 0.1.0
 */
public interface Tokens {

    /**
     * Generate a new authentication token for the current KSeF context.
     * Returns immediately with a reference number and the token value
     * (which is shown only once). The token starts in {@code Pending}
     * status and transitions to {@code Active} asynchronously after
     * KSeF activates the requested permissions; poll
     * {@link #getStatus(String)} when synchronous readiness is needed.
     *
     * <p>Tokens may only be generated in NIP or internal-identifier
     * contexts (per spec). The token's permissions are bounded by the
     * caller's own permissions — requesting permissions the caller
     * does not hold yields {@code 26001} as a typed validation error.
     *
     * @param request the token generation request (permissions +
     *     description, per {@code TokenGenerateRequest})
     * @return reference number + raw token value (returned only once)
     */
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

    /**
     * Retrieve the current status of a token by reference number.
     * Used to drive activation polling after {@link #generate} returns
     * the token reference. Possible statuses (per spec
     * {@code AuthenticationTokenStatus}): {@code Pending} (created,
     * permissions still propagating), {@code Active} (usable for
     * authentication), {@code Revoking} (revocation in progress —
     * cannot be used), {@code Revoked} (terminal — generate a new
     * token), {@code Failed} (terminal — generate a new token).
     *
     * @param referenceNumber the reference number returned by
     *     {@link #generate} (non-null)
     * @return the token's current status and metadata
     */
    TokenDetail getStatus(String referenceNumber);

    /**
     * Revoke a token by reference number. The token transitions to
     * {@code Revoking} then to {@code Revoked} asynchronously. After
     * revocation the token can no longer authenticate; existing
     * sessions established with it terminate per KSeF lifecycle.
     *
     * <p>Revoking an already-revoked or non-existent token surfaces as
     * a typed server validation error. Consumers that need to confirm
     * terminal {@code Revoked} state should poll {@link #getStatus}
     * after this call.
     *
     * @param referenceNumber the reference number of the token to
     *     revoke (non-null)
     */
    void revoke(String referenceNumber);
}
