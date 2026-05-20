/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Per-permission entry for entity grant requests. {@code canDelegate} marks
 * a permission that the recipient may pass on to other principals.
 *
 * @since 0.1.0
 */
public record EntityPermissionEntry(EntityPermissionType type, boolean canDelegate) {
}
