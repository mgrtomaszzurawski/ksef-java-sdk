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

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static EuEntityPermission from(EuEntityPermissionRaw raw) {
        var authorRaw = raw.getAuthorIdentifier();
        PermissionIdentifier authorId = new PermissionIdentifier(authorRaw.getType().getValue(), authorRaw.getValue());
        String scope = raw.getPermissionScope().getValue();
        return new EuEntityPermission(raw.getId(), authorId, raw.getVatUeIdentifier(),
                raw.getEuEntityName(), raw.getAuthorizedFingerprintIdentifier(),
                scope, raw.getDescription(), raw.getStartDate());
    }
}
