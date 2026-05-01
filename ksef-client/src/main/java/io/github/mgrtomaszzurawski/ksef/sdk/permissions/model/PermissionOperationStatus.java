/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;

/**
 * Status of a permission operation.
 *
 * @param status current operation status
 */
public record PermissionOperationStatus(StatusInfo status) {

    public static PermissionOperationStatus from(PermissionsOperationStatusResponseRaw raw) {
        return new PermissionOperationStatus(StatusInfo.from(raw.getStatus()));
    }
}
