/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;

/**
 * Result of querying EU entity permissions.
 */
public record EuEntityPermissions(List<EuEntityPermission> permissions, boolean hasMore) {

}
