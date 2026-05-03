/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Test/advanced-consumer escape hatch for accessing the {@link KsefClient}
 * internal {@link HttpRuntime} adapter and the underlying {@link SessionContext}.
 *
 * <p>This class exists solely to keep the existing test suites working after
 * {@code KsefClient.runtime()} was demoted to package-private. Consumer code
 * must NOT depend on it. A future {@code ksef-client-testkit} module will host
 * the same seam with stronger isolation; this class will be removed at that
 * time.
 *
 * @apiNote SDK-internal / test seam. Not part of the stable public API.
 * @deprecated For SDK-internal and unit-test use only. Will be moved into a
 *     separate {@code ksef-client-testkit} module in 0.2.x.
 */
@Deprecated(since = "0.1.0", forRemoval = true)
public final class KsefClientInternals {

    private KsefClientInternals() {
        // utility
    }

    /**
     * Return the internal {@link HttpRuntime} adapter held by {@code client}.
     * Used by SDK-internal unit tests that construct {@code AuthClient},
     * {@code SessionClient}, {@code SecurityClient}, {@code HttpSupport} etc.
     * directly, bypassing the public {@link KsefClient} facade.
     */
    public static HttpRuntime runtime(KsefClient client) {
        return client.internalRuntime();
    }

    /**
     * Return the {@link SessionContext} held by {@code client}. Equivalent to
     * {@code runtime(client).sessionContext()} but offered as a separate
     * accessor so callers do not need an {@link HttpRuntime} reference for
     * test-only assertions on the session state.
     */
    public static SessionContext sessionContext(KsefClient client) {
        return client.internalRuntime().sessionContext();
    }
}
