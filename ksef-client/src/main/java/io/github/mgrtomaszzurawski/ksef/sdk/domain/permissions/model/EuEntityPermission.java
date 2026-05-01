/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.EuEntityPermissionRaw;
import java.time.OffsetDateTime;

/**
 * An EU entity permission entry from query results.
 */
public record EuEntityPermission(
        String id,
        PermissionIdentifier authorIdentifier,
        String vatUeIdentifier,
        String euEntityName,
        String authorizedFingerprintIdentifier,
        String permissionScope,
        String description,
        OffsetDateTime startDate) {

    public static EuEntityPermission from(EuEntityPermissionRaw raw) {
        PermissionIdentifier authorId = raw.getAuthorIdentifier() != null
                ? new PermissionIdentifier(
                        raw.getAuthorIdentifier().getType() != null ? raw.getAuthorIdentifier().getType().getValue() : null,
                        raw.getAuthorIdentifier().getValue())
                : null;
        return new EuEntityPermission(
                raw.getId(), authorId,
                raw.getVatUeIdentifier(), raw.getEuEntityName(),
                raw.getAuthorizedFingerprintIdentifier(),
                raw.getPermissionScope() != null ? raw.getPermissionScope().getValue() : null,
                raw.getDescription(), raw.getStartDate());
    }
}
