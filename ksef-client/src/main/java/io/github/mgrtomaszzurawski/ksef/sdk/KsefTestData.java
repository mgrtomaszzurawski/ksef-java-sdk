/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataAdmin;
import java.util.Objects;

/**
 * Opt-in factory for the KSeF TEST/DEMO-only test-data administration
 * surface.
 *
 * <p>The KSeF TEST and DEMO environments expose a wide set of tooling
 * endpoints — create subjects, seed people, grant attachment
 * permissions, fiddle with session/subject limits — that have no PROD
 * counterpart. Keeping that surface OFF the main {@link KsefClient}
 * accessor list means PROD consumers never see it in autocomplete and
 * cannot accidentally call it from code paths reused across
 * environments.
 *
 * <p>Usage:
 * <pre>{@code
 * try (KsefClient client = KsefClient.builder()
 *         .environment(KsefEnvironment.TEST)
 *         .credentials(creds)
 *         .build()) {
 *     TestDataAdmin testData = KsefTestData.of(client);
 *     testData.createSubject(...);
 * }
 * }</pre>
 *
 * <p>Throws {@link IllegalArgumentException} when the client is wired to
 * {@link KsefEnvironment#PROD} — the test-data API does not exist
 * there. The check is environment-equality based: {@code custom(url)}
 * bypasses it on purpose so a non-canonical TEST gateway can still be
 * used.
 *
 * @since 1.0.0
 */
public final class KsefTestData {

    private static final String ERR_NULL_CLIENT = "client must not be null";
    private static final String ERR_PROD =
            "Test-data API is not available on KsefEnvironment.PROD — "
                    + "use TEST or DEMO for test-tenant operations.";

    private KsefTestData() {
        // factory class, no instances
    }

    /**
     * Return the test-data admin facade for the supplied client.
     *
     * @param client an open {@link KsefClient} configured for TEST/DEMO
     * @return the test-data admin facade
     * @throws NullPointerException     if {@code client} is null
     * @throws IllegalArgumentException if the client is wired to
     *     {@link KsefEnvironment#PROD}
     * @throws IllegalStateException    if the client is closed
     */
    public static TestDataAdmin of(KsefClient client) {
        Objects.requireNonNull(client, ERR_NULL_CLIENT);
        if (KsefEnvironment.PROD.equals(client.environment())) {
            throw new IllegalArgumentException(ERR_PROD);
        }
        return client.testDataInternal();
    }
}
