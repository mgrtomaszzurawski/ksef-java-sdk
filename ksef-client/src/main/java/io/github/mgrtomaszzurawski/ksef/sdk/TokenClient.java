/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.model.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.TokenGenerateBuilder;

import java.util.Objects;

import static io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport.requireSafePathSegment;

/**
 * Client for KSeF API token management — generate, list, query status, and revoke tokens.
 */
public final class TokenClient {

    private static final String PATH_TOKENS = "/api/v2/tokens";

    private static final String OP_GENERATE = "generateToken";
    private static final String OP_LIST = "listTokens";
    private static final String OP_GET_STATUS = "getTokenStatus";
    private static final String OP_REVOKE = "revokeToken";
    private static final String ERR_NULL_BUILDER = "tokenBuilder must not be null";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public TokenClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Generate a new API token with the specified permissions and description.
     *
     * @param tokenBuilder token generation builder with permissions and description
     * @return response with the generated token details
     */
    public GenerateTokenResult generate(TokenGenerateBuilder tokenBuilder) {
        Objects.requireNonNull(tokenBuilder, ERR_NULL_BUILDER);
        String token = sessionContext.token();
        GenerateTokenResponseRaw raw = http.postJsonAuthenticated(PATH_TOKENS,
                tokenBuilder.build(), token, GenerateTokenResponseRaw.class, OP_GENERATE);
        return GenerateTokenResult.from(raw);
    }

    /**
     * List all tokens for the current subject.
     *
     * @return response with the list of tokens
     */
    public TokenList list() {
        String token = sessionContext.token();
        QueryTokensResponseRaw raw = http.getAuthenticated(PATH_TOKENS, token,
                QueryTokensResponseRaw.class, OP_LIST);
        return TokenList.from(raw);
    }

    /**
     * Get the status of a specific token by reference number.
     *
     * @param referenceNumber the token reference number
     * @return token status details
     */
    public TokenDetail getStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        TokenStatusResponseRaw raw = http.getAuthenticated(PATH_TOKENS + "/" + referenceNumber, token,
                TokenStatusResponseRaw.class, OP_GET_STATUS);
        return TokenDetail.from(raw);
    }

    /**
     * Revoke a token by reference number.
     *
     * @param referenceNumber the token reference number to revoke
     */
    public void revoke(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_TOKENS + "/" + referenceNumber, token, OP_REVOKE);
    }
}
