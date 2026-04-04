/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

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

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public TokenClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Generate a new API token with the specified permissions and description.
     *
     * @param request token generation parameters (permissions, description)
     * @return response with the generated token details
     */
    public GenerateTokenResponseRaw generate(GenerateTokenRequestRaw request) {
        String token = sessionContext.token();
        return http.postJsonAuthenticated(PATH_TOKENS, request, token,
                GenerateTokenResponseRaw.class, OP_GENERATE);
    }

    /**
     * List all tokens for the current subject.
     *
     * @return response with the list of tokens
     */
    public QueryTokensResponseRaw list() {
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_TOKENS, token,
                QueryTokensResponseRaw.class, OP_LIST);
    }

    /**
     * Get the status of a specific token by reference number.
     *
     * @param referenceNumber the token reference number
     * @return token status details
     */
    public TokenStatusResponseRaw getStatus(String referenceNumber) {
        requireSafePathSegment(referenceNumber);
        String token = sessionContext.token();
        return http.getAuthenticated(PATH_TOKENS + "/" + referenceNumber, token,
                TokenStatusResponseRaw.class, OP_GET_STATUS);
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
