/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import java.time.Duration;
import java.util.Optional;

/**
 * Immutable snapshot of how a {@link io.github.mgrtomaszzurawski.ksef.sdk.KsefClient}
 * was configured at {@code build()} time. Returned by
 * {@code KsefClient.config()} so consumers can:
 *
 * <ul>
 *   <li><b>Identify</b> a client without comparing it to {@code KsefClient}
 *       directly — record equality on this snapshot is field-by-field
 *       and excludes secrets (credentials are surfaced via the masked
 *       {@link KsefCredentialsDescriptor}).</li>
 *   <li><b>Log diagnostics</b> via {@code config().toString()} — record
 *       {@code toString} prints all fields including the masked
 *       descriptor, never the raw credentials.</li>
 *   <li><b>Compare configurations</b> across two clients to confirm a
 *       multi-tenant orchestrator wired them identically — equality is
 *       structural over the eight fields, with the descriptor's
 *       per-field equality covering credentials safely.</li>
 * </ul>
 *
 * <p>{@code KsefClient} itself intentionally does <em>not</em> override
 * {@code equals}/{@code hashCode} — it is an {@link AutoCloseable}
 * resource holder with mutable session state, and value-identity on a
 * stateful Closeable would mislead callers (two clients with identical
 * config but different auth state are not interchangeable).
 *
 * <p>{@link #offlineSigningProvider()} surfaces the registered provider
 * by reference rather than via a masked descriptor: the
 * {@link OfflineSigningProvider} contract is an opaque strategy — the
 * SDK never sees the underlying private-key material directly (the
 * provider owns the certificate and key), so a masked wrapper would add
 * no security value. Two clients with different provider instances
 * compare unequal under record-equality, which matches "different
 * signing strategies are not interchangeable" intent.
 *
 * @since 1.0.0
 */
public record KsefClientConfig(
        KsefEnvironment environment,
        KsefCredentialsDescriptor credentials,
        Duration connectTimeout,
        Duration readTimeout,
        Duration invoiceVerificationTimeout,
        RetryPolicy retryPolicy,
        FeaturePolicy features,
        Optional<OfflineSigningProvider> offlineSigningProvider) {
}
