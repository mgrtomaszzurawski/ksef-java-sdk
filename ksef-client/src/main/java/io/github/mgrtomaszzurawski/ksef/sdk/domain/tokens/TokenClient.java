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
     * List every token, following the {@code x-continuation-token} cursor
     * internally. Codex round-9 manual-validation A.4.1; abstract per Codex
     * 2026-05-05 F4.
     */
    java.util.List<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> listAll();

    TokenDetail getStatus(String referenceNumber);
    void revoke(String referenceNumber);

}
