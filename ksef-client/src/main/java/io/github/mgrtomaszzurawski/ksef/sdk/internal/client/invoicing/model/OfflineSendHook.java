/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import org.jspecify.annotations.Nullable;

/**
 * Offline-send wiring: the {@link OfflineSigningProvider} configured on
 * {@code KsefClient.Builder.offlineSigning(...)} plus the seller NIP
 * resolved from credentials. Both nullable — {@code sendOffline} throws
 * a typed exception when either is missing.
 *
 * @since 1.0.0
 */
public record OfflineSendHook(
        @Nullable OfflineSigningProvider provider,
        @Nullable String sellerNip) {

    /** Empty hook — used when offline signing is not configured on the client. */
    public static OfflineSendHook empty() {
        return new OfflineSendHook(null, null);
    }
}
