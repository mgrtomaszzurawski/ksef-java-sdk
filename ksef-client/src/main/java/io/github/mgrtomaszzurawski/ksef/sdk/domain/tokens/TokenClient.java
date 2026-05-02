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

    public GenerateTokenResult generate(TokenGenerateBuilder tokenBuilder);

    public TokenList list();

    public TokenDetail getStatus(String referenceNumber);

    public void revoke(String referenceNumber);

}