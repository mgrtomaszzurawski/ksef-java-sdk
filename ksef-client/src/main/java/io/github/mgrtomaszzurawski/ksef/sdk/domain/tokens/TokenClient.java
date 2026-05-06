/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
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
     * Single-page list — keeps the original signature including the
     * {@code continuationToken} accessor.
     */
    TokenList list();

    /**
     * Stream every token. Pages are fetched lazily, following the
     * {@code x-continuation-token} cursor returned by the server.
     * Caller controls memory by limiting / collecting downstream.
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> streamTokens();

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);

    /**
     * Codex 2026-05-05 #10 / F7 — generate a token and poll
     * {@link #getStatus(String)} until terminal. Terminal statuses for
     * a freshly generated token are {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus#ACTIVE ACTIVE}
     * (success) or {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus#FAILED FAILED}.
     * Throws {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAsyncTimeoutException}
     * on timeout.
     *
     * @since 1.0.0
     */
    default TokenDetail generateAndAwait(TokenGenerateBuilder tokenBuilder, java.time.Duration timeout) {
        GenerateTokenResult result = generate(tokenBuilder);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.AsyncOperationAwaiter.awaitTerminal(
                new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.AsyncOperationAwaiter.Config<>(
                        "generateToken",
                        () -> getStatus(result.referenceNumber()),
                        detail -> detail.status() != null
                                && (detail.status() == io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus.ACTIVE
                                    || detail.status() == io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus.FAILED),
                        TokenDetail::status,
                        timeout,
                        null));
    }

}
