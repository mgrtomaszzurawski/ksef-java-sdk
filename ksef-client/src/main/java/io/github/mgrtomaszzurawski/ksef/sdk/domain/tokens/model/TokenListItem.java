/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

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

}
