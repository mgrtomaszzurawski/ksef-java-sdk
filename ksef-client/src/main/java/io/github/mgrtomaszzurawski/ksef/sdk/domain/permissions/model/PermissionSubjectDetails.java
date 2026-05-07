/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import org.jspecify.annotations.Nullable;

/**
 * Details about a permission subject (person or entity).
 *
 * @param firstName first name (persons only; null for entities)
 * @param surname surname (persons only; null for entities)
 * @param fullName full/trade name (entities only; null for persons)
 *
 * @since 1.0.0
 */
public record PermissionSubjectDetails(@Nullable String firstName, @Nullable String surname, @Nullable String fullName) {
}
