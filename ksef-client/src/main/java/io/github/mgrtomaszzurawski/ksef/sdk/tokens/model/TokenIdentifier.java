/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens.model;
/**
 * Identifier used in token author/context identification.
 *
 * @param type identifier type name (e.g. "Nip", "Pesel", "Fingerprint", "InternalId")
 * @param value identifier value
 */
public record TokenIdentifier(String type, String value) {
}
