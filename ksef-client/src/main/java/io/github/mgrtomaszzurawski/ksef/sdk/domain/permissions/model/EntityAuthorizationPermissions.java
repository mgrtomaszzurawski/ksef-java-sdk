/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;

/**
 * Result of querying entity authorization permissions.
 */
public record EntityAuthorizationPermissions(List<EntityAuthorizationGrant> authorizationGrants, boolean hasMore) {

}
