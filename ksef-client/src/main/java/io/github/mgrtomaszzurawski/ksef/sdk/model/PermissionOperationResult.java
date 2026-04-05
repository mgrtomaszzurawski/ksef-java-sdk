/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationResponseRaw;

/**
 * Result of a permission grant or revoke operation.
 *
 * @param referenceNumber operation reference number for status polling
 */
public record PermissionOperationResult(String referenceNumber) {

    public static PermissionOperationResult from(PermissionsOperationResponseRaw raw) {
        return new PermissionOperationResult(raw.getReferenceNumber());
    }
}
