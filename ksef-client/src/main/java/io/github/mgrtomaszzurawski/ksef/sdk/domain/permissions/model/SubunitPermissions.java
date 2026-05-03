/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;

/**
 * Result of querying subunit permissions.
 *
 * @param permissions list of subunit permissions
 * @param hasMore whether more results are available
 */
public record SubunitPermissions(List<SubunitPermission> permissions, boolean hasMore) {

}
