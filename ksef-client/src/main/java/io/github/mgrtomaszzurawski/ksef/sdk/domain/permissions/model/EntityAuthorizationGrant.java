/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EntityAuthorizationGrantRaw;
import java.time.OffsetDateTime;

/**
 * An entity authorization grant from query results.
 */
public record EntityAuthorizationGrant(
        String id,
        PermissionIdentifier authorIdentifier,
        PermissionIdentifier authorizedEntityIdentifier,
        PermissionIdentifier authorizingEntityIdentifier,
        String authorizationScope,
        String description,
        OffsetDateTime startDate) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static EntityAuthorizationGrant from(EntityAuthorizationGrantRaw raw) {
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = authorRaw != null
                ? new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue())
                : null;
        var authzRaw = raw.getAuthorizedEntityIdentifier();
        PermissionIdentifier authzEntityId = new PermissionIdentifier(authzRaw.getType().getValue(), authzRaw.getValue());
        var authingRaw = raw.getAuthorizingEntityIdentifier();
        PermissionIdentifier authingEntityId = new PermissionIdentifier(authingRaw.getType().getValue(), authingRaw.getValue());
        String authScope = raw.getAuthorizationScope().getValue();
        return new EntityAuthorizationGrant(raw.getId(), authorId, authzEntityId, authingEntityId,
                authScope, raw.getDescription(), raw.getStartDate());
    }
}
