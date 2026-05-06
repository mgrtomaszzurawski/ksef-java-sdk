/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Result of a permission grant or revoke operation.
 *
 * @param referenceNumber operation reference number for status polling
 *
 * @since 1.0.0
 */
public record PermissionOperationResult(String referenceNumber) {

}
