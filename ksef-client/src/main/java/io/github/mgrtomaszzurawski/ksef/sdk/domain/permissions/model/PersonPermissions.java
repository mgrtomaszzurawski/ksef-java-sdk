/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;

/**
 * Result of querying person permissions.
 *
 * @param permissions list of person permissions
 * @param hasMore whether more results are available
 *
 * @since 1.0.0
 */
public record PersonPermissions(List<PersonPermission> permissions, boolean hasMore) {

}
