/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Lifecycle status of a KSeF API token.
 */
public enum TokenStatus {

    PENDING,
    ACTIVE,
    REVOKING,
    REVOKED,
    FAILED;

}
