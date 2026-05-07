/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import org.jspecify.annotations.Nullable;

/**
 * Status of a permission operation.
 *
 * @param status current operation status (null when server omits status block)
 *
 * @since 1.0.0
 */
public record PermissionOperationStatus(@Nullable StatusInfo status) {

}
