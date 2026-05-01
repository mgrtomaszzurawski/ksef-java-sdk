/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens;

import io.github.mgrtomaszzurawski.ksef.client.model.TokenStatusResponseRaw;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detailed information about a KSeF API token.
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
public record TokenDetail(
        String referenceNumber,
        TokenIdentifier authorIdentifier,
        TokenIdentifier contextIdentifier,
        String description,
        List<TokenPermissionType> requestedPermissions,
        OffsetDateTime dateCreated,
        OffsetDateTime lastUseDate,
        TokenStatus status,
        List<String> statusDetails) {

    public static TokenDetail from(TokenStatusResponseRaw raw) {
        TokenIdentifier author = raw.getAuthorIdentifier() != null
                ? new TokenIdentifier(
                        raw.getAuthorIdentifier().getType() != null ? raw.getAuthorIdentifier().getType().getValue() : null,
                        raw.getAuthorIdentifier().getValue())
                : null;
        TokenIdentifier context = raw.getContextIdentifier() != null
                ? new TokenIdentifier(
                        raw.getContextIdentifier().getType() != null ? raw.getContextIdentifier().getType().getValue() : null,
                        raw.getContextIdentifier().getValue())
                : null;
        List<TokenPermissionType> perms = raw.getRequestedPermissions() != null
                ? raw.getRequestedPermissions().stream().map(TokenPermissionType::from).toList()
                : List.of();
        return new TokenDetail(
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
