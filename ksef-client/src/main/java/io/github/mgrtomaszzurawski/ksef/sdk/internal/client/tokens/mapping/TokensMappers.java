/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationTokenStatusRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenPermissionTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Internal mappers from generated {@code *Raw} types to public tokens
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 0.1.0
 */
public final class TokensMappers {

    private TokensMappers() { }

    public static GenerateTokenRequestRaw toGenerateTokenRequestRaw(TokenGenerateRequest request) {
        GenerateTokenRequestRaw rawValue = new GenerateTokenRequestRaw();
        rawValue.setDescription(request.description());
        List<TokenPermissionTypeRaw> permissions = new java.util.ArrayList<>(request.permissions().size());
        for (TokenPermissionType type : request.permissions()) {
            permissions.add(toTokenPermissionTypeRaw(type));
        }
        rawValue.setPermissions(permissions);
        return rawValue;
    }

    public static TokenPermissionTypeRaw toTokenPermissionTypeRaw(TokenPermissionType value) {
        return switch (value) {
            case INVOICE_READ -> TokenPermissionTypeRaw.INVOICE_READ;
            case INVOICE_WRITE -> TokenPermissionTypeRaw.INVOICE_WRITE;
            case CREDENTIALS_READ -> TokenPermissionTypeRaw.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> TokenPermissionTypeRaw.CREDENTIALS_MANAGE;
            case SUBUNIT_MANAGE -> TokenPermissionTypeRaw.SUBUNIT_MANAGE;
            case ENFORCEMENT_OPERATIONS -> TokenPermissionTypeRaw.ENFORCEMENT_OPERATIONS;
            case INTROSPECTION -> TokenPermissionTypeRaw.INTROSPECTION;
        };
    }

    public static GenerateTokenResult toGenerateTokenResult(GenerateTokenResponseRaw rawValue) {
        return new GenerateTokenResult(rawValue.getReferenceNumber(), rawValue.getToken());
    }

    public static TokenDetail toTokenDetail(TokenStatusResponseRaw rawValue) {
        var authorRaw = rawValue.getAuthorIdentifier();
        TokenIdentifier author = new TokenIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        var ctxRaw = rawValue.getContextIdentifier();
        TokenIdentifier context = new TokenIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue());
        List<TokenPermissionType> perms = rawValue.getRequestedPermissions().stream().map(TokensMappers::toTokenPermissionType).toList();
        return new TokenDetail(
                rawValue.getReferenceNumber(),
                author,
                context,
                rawValue.getDescription(),
                perms,
                rawValue.getDateCreated(),
                rawValue.getLastUseDate(),
                toTokenStatus(rawValue.getStatus()),
                rawValue.getStatusDetails() != null ? List.copyOf(rawValue.getStatusDetails()) : List.of());
    }

    public static TokenList toTokenList(QueryTokensResponseRaw rawValue) {
        List<TokenListItem> mapped = rawValue.getTokens().stream().map(TokensMappers::toTokenListItem).toList();
        return new TokenList(rawValue.getContinuationToken(), mapped);
    }

    public static TokenListItem toTokenListItem(QueryTokensResponseItemRaw rawValue) {
        var authorRaw = rawValue.getAuthorIdentifier();
        TokenIdentifier author = new TokenIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        var ctxRaw = rawValue.getContextIdentifier();
        TokenIdentifier context = new TokenIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue());
        List<TokenPermissionType> perms = rawValue.getRequestedPermissions().stream().map(TokensMappers::toTokenPermissionType).toList();
        return new TokenListItem(
                rawValue.getReferenceNumber(),
                author,
                context,
                rawValue.getDescription(),
                perms,
                rawValue.getDateCreated(),
                rawValue.getLastUseDate(),
                toTokenStatus(rawValue.getStatus()),
                rawValue.getStatusDetails() != null ? List.copyOf(rawValue.getStatusDetails()) : List.of());
    }

    public static @Nullable TokenPermissionType toTokenPermissionType(@Nullable TokenPermissionTypeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case INVOICE_READ -> TokenPermissionType.INVOICE_READ;
            case INVOICE_WRITE -> TokenPermissionType.INVOICE_WRITE;
            case CREDENTIALS_READ -> TokenPermissionType.CREDENTIALS_READ;
            case CREDENTIALS_MANAGE -> TokenPermissionType.CREDENTIALS_MANAGE;
            case SUBUNIT_MANAGE -> TokenPermissionType.SUBUNIT_MANAGE;
            case ENFORCEMENT_OPERATIONS -> TokenPermissionType.ENFORCEMENT_OPERATIONS;
            case INTROSPECTION -> TokenPermissionType.INTROSPECTION;
        };
    }

    public static @Nullable TokenStatus toTokenStatus(@Nullable AuthenticationTokenStatusRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case PENDING -> TokenStatus.PENDING;
            case ACTIVE -> TokenStatus.ACTIVE;
            case REVOKING -> TokenStatus.REVOKING;
            case REVOKED -> TokenStatus.REVOKED;
            case FAILED -> TokenStatus.FAILED;
        };
    }

}
