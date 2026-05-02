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

    public static EntityAuthorizationGrant from(EntityAuthorizationGrantRaw raw) {
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = null;
        if (authorRaw != null) {
            String type = authorRaw.getType() != null ? authorRaw.getType().getValue() : null;
            authorId = new PermissionIdentifier(type, authorRaw.getValue());
        }
        var authzRaw = raw.getAuthorizedEntityIdentifier();
        PermissionIdentifier authzEntityId = null;
        if (authzRaw != null) {
            String type = authzRaw.getType() != null ? authzRaw.getType().getValue() : null;
            authzEntityId = new PermissionIdentifier(type, authzRaw.getValue());
        }
        var authingRaw = raw.getAuthorizingEntityIdentifier();
        PermissionIdentifier authingEntityId = null;
        if (authingRaw != null) {
            String type = authingRaw.getType() != null ? authingRaw.getType().getValue() : null;
            authingEntityId = new PermissionIdentifier(type, authingRaw.getValue());
        }
        String authScope = raw.getAuthorizationScope().getValue();
        return new EntityAuthorizationGrant(raw.getId(), authorId, authzEntityId, authingEntityId,
                authScope, raw.getDescription(), raw.getStartDate());
    }
}
