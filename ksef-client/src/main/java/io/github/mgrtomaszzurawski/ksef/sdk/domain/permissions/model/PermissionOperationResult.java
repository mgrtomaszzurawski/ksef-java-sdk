/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Result of a permission grant or revoke operation.
 *
 * @param referenceNumber operation reference number for status polling
 */
public record PermissionOperationResult(String referenceNumber) {

}
