/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Result of generating a new KSeF API token.
 *
 * @param referenceNumber operation reference number
 * @param token the generated token string
 */
public record GenerateTokenResult(String referenceNumber, String token) {

}
