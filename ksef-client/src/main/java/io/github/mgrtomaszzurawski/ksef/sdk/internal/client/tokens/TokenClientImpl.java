/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.TokenClient;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.mapping.TokensMappers;

/**
 * Client for KSeF API token management — generate, list, query status, and revoke tokens.
 */
public final class TokenClientImpl implements TokenClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenClientImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_TOKENS = ApiPaths.TOKENS;
    private static final String PATH_SEPARATOR = "/";

    private static final String OP_GENERATE = "generateToken";
    private static final String OP_LIST = "listTokens";
    private static final String OP_GET_STATUS = "getTokenStatus";
    private static final String OP_REVOKE = "revokeToken";
    private static final String ERR_NULL_BUILDER = "tokenBuilder must not be null";

    private final HttpSupport http;

    public TokenClientImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Generate a new API token with the specified permissions and description.
     *
     * @param tokenBuilder token generation builder with permissions and description
     * @return response with the generated token details
     */
    @Override
    public GenerateTokenResult generate(TokenGenerateBuilder tokenBuilder) {
        LOGGER.debug(LOG_CALL, OP_GENERATE);
        Objects.requireNonNull(tokenBuilder, ERR_NULL_BUILDER);
        String token = http.requireToken();
        GenerateTokenResponseRaw rawValue = http.postJsonAuthenticated(PATH_TOKENS,
                TokensMappers.toGenerateTokenRequestRaw(tokenBuilder.build()), token,
                GenerateTokenResponseRaw.class, OP_GENERATE);
        return TokensMappers.toGenerateTokenResult(rawValue);
    }

    /**
     * List all tokens for the current subject.
     *
     * @return response with the list of tokens
     */
    @Override
    public TokenList list() {
        LOGGER.debug(LOG_CALL, OP_LIST);
        String token = http.requireToken();
        QueryTokensResponseRaw rawValue = http.getAuthenticated(PATH_TOKENS, token,
                QueryTokensResponseRaw.class, OP_LIST);
        return TokensMappers.toTokenList(rawValue);
    }

    /**
     * Get the status of a specific token by reference number.
     *
     * @param referenceNumber the token reference number
     * @return token status details
     */
    @Override
    public TokenDetail getStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_GET_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        TokenStatusResponseRaw rawValue = http.getAuthenticated(PATH_TOKENS + PATH_SEPARATOR + referenceNumber, token,
                TokenStatusResponseRaw.class, OP_GET_STATUS);
        return TokensMappers.toTokenDetail(rawValue);
    }

    /**
     * Revoke a token by reference number.
     *
     * @param referenceNumber the token reference number to revoke
     */
    @Override
    public void revoke(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_REVOKE, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        http.deleteAuthenticated(PATH_TOKENS + PATH_SEPARATOR + referenceNumber, token, OP_REVOKE);
    }
}
