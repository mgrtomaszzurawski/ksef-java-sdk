/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Details about an EU entity referenced by an {@link EuEntityPermission}.
 *
 * @param fullName registered legal name of the EU entity
 * @param address single-line address of the EU entity
 *
 * @since 0.1.0
 */
public record PermissionEuEntityDetails(String fullName, String address) {
}
