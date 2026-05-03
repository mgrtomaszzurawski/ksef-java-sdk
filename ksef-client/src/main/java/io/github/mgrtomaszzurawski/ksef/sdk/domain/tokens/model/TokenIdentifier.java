/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;
/**
 * Identifier used in token author/context identification.
 *
 * @param type identifier type name (e.g. "Nip", "Pesel", "Fingerprint", "InternalId")
 * @param value identifier value
 */
public record TokenIdentifier(String type, String value) {
}
