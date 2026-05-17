/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Root package of the KSeF SDK public API. The single entry point is
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.KsefClient} (with its
 * configuration {@code Builder}).
 *
 * <p>Operational APIs live in {@code sdk.domain.<feature>} sub-packages,
 * reached via accessor methods on {@code KsefClient} (for example
 * {@code client.invoices()}, {@code client.permissions()},
 * {@code client.authSessions()}, {@code client.testData()}).
 *
 * <p>Configuration types (environment, credentials, retry policy, feature
 * policy, authorization policy) live in {@code sdk.config}; shared value
 * types ({@code StatusInfo}, {@code TokenInfo},
 * {@code PublicKeyCertificate}) live in {@code sdk.common}; the typed
 * exception hierarchy lives in {@code sdk.exception}; the XSD-driven
 * invoice validator and XML utility surface live in {@code sdk.crypto}.
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk;
