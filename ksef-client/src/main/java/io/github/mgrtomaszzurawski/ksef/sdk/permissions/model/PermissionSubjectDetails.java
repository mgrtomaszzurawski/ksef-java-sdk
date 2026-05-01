/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;
/**
 * Details about a permission subject (person or entity).
 *
 * @param firstName first name (persons only)
 * @param surname surname (persons only)
 * @param fullName full/trade name (entities only)
 */
public record PermissionSubjectDetails(String firstName, String surname, String fullName) {
}
