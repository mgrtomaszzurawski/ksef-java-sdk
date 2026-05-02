/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryTokensResponseItemRaw;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Token summary in a token list query.
 *
 * @param referenceNumber token reference number
 * @param authorIdentifier who created the token
 * @param contextIdentifier context (NIP) the token is for
 * @param description token description
 * @param requestedPermissions permissions granted to the token
 * @param dateCreated when the token was created
 * @param lastUseDate when the token was last used
 * @param status current lifecycle status
 * @param statusDetails additional status details
 */
public record TokenListItem(
        String referenceNumber,
        TokenIdentifier authorIdentifier,
        TokenIdentifier contextIdentifier,
        String description,
        List<TokenPermissionType> requestedPermissions,
        OffsetDateTime dateCreated,
        OffsetDateTime lastUseDate,
        TokenStatus status,
        List<String> statusDetails) {

    public static TokenListItem from(QueryTokensResponseItemRaw raw) {
        var authorRaw = raw.getAuthorIdentifier();
        TokenIdentifier author = new TokenIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        var ctxRaw = raw.getContextIdentifier();
        TokenIdentifier context = new TokenIdentifier(ctxRaw.getType().getValue(), ctxRaw.getValue());
        List<TokenPermissionType> perms = raw.getRequestedPermissions().stream().map(TokenPermissionType::from).toList();
        return new TokenListItem(
                raw.getReferenceNumber(),
                author,
                context,
                raw.getDescription(),
                perms,
                raw.getDateCreated(),
                raw.getLastUseDate(),
                TokenStatus.from(raw.getStatus()),
                raw.getStatusDetails() != null ? List.copyOf(raw.getStatusDetails()) : List.of());
    }
}
