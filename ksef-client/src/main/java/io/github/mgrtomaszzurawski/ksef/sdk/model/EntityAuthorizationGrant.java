/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

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
        PermissionIdentifier authorId = raw.getAuthorIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorIdentifier().getType() != null ? raw.getAuthorIdentifier().getType().getValue() : null,
                        raw.getAuthorIdentifier().getValue())
                : null;
        PermissionIdentifier authzEntityId = raw.getAuthorizedEntityIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorizedEntityIdentifier().getType() != null ? raw.getAuthorizedEntityIdentifier().getType().getValue() : null,
                        raw.getAuthorizedEntityIdentifier().getValue())
                : null;
        PermissionIdentifier authingEntityId = raw.getAuthorizingEntityIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorizingEntityIdentifier().getType() != null ? raw.getAuthorizingEntityIdentifier().getType().getValue() : null,
                        raw.getAuthorizingEntityIdentifier().getValue())
                : null;
        return new EntityAuthorizationGrant(
                raw.getId(), authorId, authzEntityId, authingEntityId,
                raw.getAuthorizationScope() != null ? raw.getAuthorizationScope().getValue() : null,
                raw.getDescription(), raw.getStartDate());
    }
}
