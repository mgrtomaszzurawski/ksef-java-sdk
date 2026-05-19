/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.Tokens;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.mapping.TokensMappers;

/**
 * Client for KSeF API token management — generate, list, query status, and revoke tokens.
 *
 * @since 1.0.0
 */
public final class TokensImpl implements Tokens {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokensImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_TOKENS = ApiPaths.TOKENS;
    private static final String PATH_SEPARATOR = "/";

    private static final String OP_GENERATE = "generateToken";
    private static final String OP_LIST = "listTokens";
    private static final String OP_GET_STATUS = "getTokenStatus";
    private static final String OP_REVOKE = "revokeToken";
    private static final String ERR_NULL_REQUEST = "request must not be null";
    private static final String ERR_NULL_FILTER = "filter must not be null";

    private static final String QUERY_PARAM_SEPARATOR_FIRST = "?";
    private static final String QUERY_PARAM_SEPARATOR = "&";
    private static final String QUERY_PARAM_EQUALS = "=";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_AUTHOR_IDENTIFIER = "authorIdentifier";
    private static final String PARAM_AUTHOR_IDENTIFIER_TYPE = "authorIdentifierType";
    private static final String PARAM_PAGE_SIZE = "pageSize";

    private final HttpSupport http;

    public TokensImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Generate a new API token with the specified permissions and description.
     *
     * @param request token generation request with permissions and description
     * @return response with the generated token details
     */
    @Override
    public GenerateTokenResult generate(TokenGenerateRequest request) {
        LOGGER.debug(LOG_CALL, OP_GENERATE);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        String token = http.requireToken();
        GenerateTokenResponseRaw rawValue = http.postJsonAuthenticated(PATH_TOKENS,
                TokensMappers.toGenerateTokenRequestRaw(request), token,
                GenerateTokenResponseRaw.class, OP_GENERATE);
        return TokensMappers.toGenerateTokenResult(rawValue);
    }

    @Override
    public TokenList queryTokens(TokenQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_LIST);
        String token = http.requireToken();
        String path = PATH_TOKENS + buildQueryString(filter);
        QueryTokensResponseRaw rawValue = filter.continuationToken() == null
                ? http.getAuthenticated(path, token, QueryTokensResponseRaw.class, OP_LIST)
                : http.getAuthenticated(path, token, QueryTokensResponseRaw.class, OP_LIST,
                        SessionClient.HEADER_CONTINUATION_TOKEN,
                        filter.continuationToken());
        return TokensMappers.toTokenList(rawValue);
    }

    @Override
    public java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem> streamTokens(TokenQueryRequest filter) {
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        LOGGER.debug(LOG_CALL, OP_LIST);
        String pathWithFilters = PATH_TOKENS + buildQueryString(filter);
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.cursorStream(continuationToken -> {
            String accessToken = http.requireToken();
            QueryTokensResponseRaw rawValue = continuationToken == null
                    ? http.getAuthenticated(pathWithFilters, accessToken, QueryTokensResponseRaw.class, OP_LIST)
                    : http.getAuthenticated(pathWithFilters, accessToken, QueryTokensResponseRaw.class, OP_LIST,
                            SessionClient.HEADER_CONTINUATION_TOKEN,
                            continuationToken);
            TokenList page = TokensMappers.toTokenList(rawValue);
            return new io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator.CursorPage<>(
                    page.tokens(), page.continuationToken());
        });
    }

    private static String buildQueryString(TokenQueryRequest filter) {
        StringBuilder query = new StringBuilder();
        for (TokenStatus status : filter.statuses()) {
            appendParam(query, PARAM_STATUS, toWireStatus(status));
        }
        if (filter.description() != null) {
            appendParam(query, PARAM_DESCRIPTION, filter.description());
        }
        if (filter.authorIdentifier() != null) {
            appendParam(query, PARAM_AUTHOR_IDENTIFIER, filter.authorIdentifier());
        }
        if (filter.authorIdentifierType() != null) {
            appendParam(query, PARAM_AUTHOR_IDENTIFIER_TYPE, filter.authorIdentifierType().wireValue());
        }
        if (filter.pageSize() != null) {
            appendParam(query, PARAM_PAGE_SIZE, filter.pageSize().toString());
        }
        return query.toString();
    }

    private static void appendParam(StringBuilder query, String name, String value) {
        query.append(query.isEmpty() ? QUERY_PARAM_SEPARATOR_FIRST : QUERY_PARAM_SEPARATOR)
                .append(name).append(QUERY_PARAM_EQUALS)
                .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Map SDK enum to spec PascalCase wire value. */
    private static String toWireStatus(TokenStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case ACTIVE -> "Active";
            case REVOKING -> "Revoking";
            case REVOKED -> "Revoked";
            case FAILED -> "Failed";
        };
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
