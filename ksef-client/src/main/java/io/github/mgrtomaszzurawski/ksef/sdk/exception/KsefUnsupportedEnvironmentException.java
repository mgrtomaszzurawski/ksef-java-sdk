/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;

/**
 * Thrown when an operation is invoked against a KSeF environment that
 * does not support it. Surfaced before any wire traffic so the caller
 * sees the env mismatch as a typed local error rather than a generic
 * server rejection.
 *
 * <p>Concrete trigger sites:
 * <ul>
 *   <li>{@code FormCode.assertAllowedOn(KsefEnvironment)} — FA(2) is
 *       accepted only on the TEST environment; DEMO and PROD reject it
 *       in favour of FA(3).</li>
 *   <li>{@code client.testData().*} — KSeF test-data administration
 *       endpoints exist on TEST and DEMO; PROD has no such endpoints
 *       and the SDK fails fast rather than letting the call reach the
 *       server.</li>
 * </ul>
 *
 * <p>Distinct from {@link IllegalArgumentException} because the problem
 * is the <em>client's environment configuration</em>, not the value of
 * an individual argument. Consumers can branch on this type to
 * distinguish env misconfiguration from genuine bad-argument errors.
 *
 * @since 1.0.0
 */
public class KsefUnsupportedEnvironmentException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefUnsupportedEnvironmentException(String message) {
        super(message, null);
    }
}
