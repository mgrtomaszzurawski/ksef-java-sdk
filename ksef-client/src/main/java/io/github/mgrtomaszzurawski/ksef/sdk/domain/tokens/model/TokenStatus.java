/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

/**
 * Lifecycle status of a KSeF API token.
 *
 * @since 0.1.0
 */
public enum TokenStatus {

    PENDING,
    ACTIVE,
    REVOKING,
    REVOKED,
    FAILED;

}
