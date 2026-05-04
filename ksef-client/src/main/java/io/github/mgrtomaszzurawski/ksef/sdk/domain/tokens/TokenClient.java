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
 */
public interface TokenClient {

    GenerateTokenResult generate(TokenGenerateBuilder tokenBuilder);

    /**
     * Single-page list — keeps the original signature including the
     * {@code continuationToken} accessor.
     */
    TokenList list();

    /**
     * List every token, following the {@code x-continuation-token} cursor
     * internally. Codex round-9 manual-validation A.4.1 — typed SDKs should
     * not force consumers to compose their own pagination loops.
     *
     * <p>Default fallback returns the first page's items only (the
     * single-page {@link #list()} accepts no continuation token, so
     * looping would refetch the same page forever). The real impl in
     * {@code TokenClientImpl} overrides with cursor iteration. Default
     * exists for source compatibility with external test doubles.
     */
    default java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> listAll() {
        return java.util.List.copyOf(list().tokens());
    }

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);

}
