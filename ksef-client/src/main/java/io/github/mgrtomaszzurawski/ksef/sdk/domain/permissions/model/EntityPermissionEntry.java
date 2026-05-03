/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Per-permission entry for entity grant requests. {@code canDelegate} marks
 * a permission that the recipient may pass on to other principals.
 */
public record EntityPermissionEntry(EntityPermissionType type, boolean canDelegate) {
}
