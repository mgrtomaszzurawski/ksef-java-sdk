/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;
/**
 * Identifier used in permission operations. The type string comes from
 * the KSeF API enum (e.g. "Nip", "Pesel", "InternalId", "Fingerprint").
 *
 * @param type identifier type name
 * @param value identifier value
 */
public record PermissionIdentifier(String type, String value) {
}
