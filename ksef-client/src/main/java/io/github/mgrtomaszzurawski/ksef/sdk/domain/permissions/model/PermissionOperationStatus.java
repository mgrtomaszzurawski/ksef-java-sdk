/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;

/**
 * Status of a permission operation.
 *
 * @param status current operation status
 */
public record PermissionOperationStatus(StatusInfo status) {

}
