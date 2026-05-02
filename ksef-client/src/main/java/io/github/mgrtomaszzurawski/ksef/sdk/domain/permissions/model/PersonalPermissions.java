/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;

/**
 * Result of querying personal permissions.
 *
 * @param permissions list of personal permissions
 * @param hasMore whether more results are available
 */
public record PersonalPermissions(List<PersonalPermission> permissions, boolean hasMore) {

}
