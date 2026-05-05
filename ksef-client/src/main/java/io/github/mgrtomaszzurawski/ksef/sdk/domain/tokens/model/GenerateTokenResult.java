/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Result of generating a new KSeF API token.
 *
 * @param referenceNumber operation reference number
 * @param token the generated token string
 *
 * @since 1.0.0
 */
public record GenerateTokenResult(String referenceNumber, String token) {

}
