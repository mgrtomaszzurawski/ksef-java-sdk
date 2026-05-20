/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Root package of the KSeF SDK public API. The single entry point is
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.KsefClient} (with its
 * configuration {@code Builder}).
 *
 * <h2>Preview release notice</h2>
 * <p><strong>This SDK is an unofficial, solo-developed preview release.</strong></p>
 * <ul>
 *   <li><strong>Not affiliated</strong> with Ministerstwo Finansów, CIRFMF,
 *       or any institution operating the KSeF system.</li>
 *   <li><strong>Solo developer</strong> — one-person project, no team SLA,
 *       no commercial support, no liability.</li>
 *   <li><strong>API may change</strong> between {@code 0.x} releases without
 *       backward compatibility (per semver {@code 0.y.z} allowance).</li>
 *   <li><strong>Use at your own risk</strong> — AGPL-3.0 disclaimer of
 *       warranties applies (LICENSE.txt sections 15–16).</li>
 *   <li>Verify behaviour against your KSeF environment before relying on
 *       this SDK for production invoice flows.</li>
 * </ul>
 *
 * <p>The official Polish e-invoicing SDK is published by CIRFMF at
 * <a href="https://github.com/CIRFMF/ksef-client-java">CIRFMF/ksef-client-java</a>.</p>
 *
 * <h2>Package layout</h2>
 * <p>Operational APIs live in {@code sdk.domain.<feature>} sub-packages,
 * reached via accessor methods on {@code KsefClient} (for example
 * {@code client.invoices()}, {@code client.permissions()},
 * {@code client.authSessions()}, {@code client.testData()}).
 *
 * <p>Configuration types (environment, credentials, retry policy, feature
 * policy, authorization policy) live in {@code sdk.config}; cross-domain
 * value/envelope types ({@code StatusInfo}, {@code KsefNumber}) live in
 * {@code sdk.core}; the typed exception hierarchy lives in
 * {@code sdk.exception}.
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk;
