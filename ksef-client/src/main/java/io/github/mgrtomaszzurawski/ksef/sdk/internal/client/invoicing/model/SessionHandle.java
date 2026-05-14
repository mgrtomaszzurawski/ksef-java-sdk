/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;

/**
 * The wire-side state of an open KSeF online session: server reference
 * number plus the symmetric encryption material agreed at session open.
 * Bundles four parameters that flow together through the online-session
 * lifecycle.
 *
 * @since 1.0.0
 */
public record SessionHandle(
        SessionClient client,
        String referenceNumber,
        byte[] aesKey,
        byte[] initVector) {
}
