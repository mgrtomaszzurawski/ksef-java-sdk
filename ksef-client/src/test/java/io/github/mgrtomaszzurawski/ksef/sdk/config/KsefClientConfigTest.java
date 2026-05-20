/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.TestCertificates;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link KsefClientConfig#offlineSigningProvider()} record-component
 * shape. R2 reshaped it from the prior {@code boolean
 * hasOfflineSigningProvider} flag to {@code Optional<OfflineSigningProvider>}
 * so consumers can both check presence AND retrieve the configured
 * provider without going through a separate accessor on {@code KsefClient}.
 *
 * <p>Two cases pinned: client built without
 * {@link KsefClient.Builder#offlineSigning(OfflineSigningProvider)} returns
 * {@code Optional.empty()}; client built with it returns the
 * {@code Optional} carrying the same provider reference.
 */
class KsefClientConfigTest {

    private static final String TEST_NIP = "1111111111";
    private static final String UNUSED_BASE_URL = "http://localhost:1/v2";

    private static OfflineSigningProvider provider;

    @BeforeAll
    static void initProvider() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        provider = OfflineSigningProvider.fromPrivateKey(
                new KsefCertificate(pair.certificate(), pair.privateKey()));
    }

    @Test
    void config_whenBuiltWithoutOfflineSigning_offlineSigningProviderIsEmpty() {
        try (KsefClient client = KsefClient.builder()
                .environment(KsefEnvironment.custom(UNUSED_BASE_URL))
                .credentials(new KsefTokenCredentials("test-ksef-token", TEST_NIP))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build()) {

            KsefClientConfig config = client.config();
            assertFalse(config.offlineSigningProvider().isPresent(),
                    "Without .offlineSigning(...) the config snapshot's provider must be empty.");
        }
    }

    @Test
    void config_whenBuiltWithOfflineSigning_offlineSigningProviderCarriesSameReference() {
        try (KsefClient client = KsefClient.builder()
                .environment(KsefEnvironment.custom(UNUSED_BASE_URL))
                .credentials(new KsefTokenCredentials("test-ksef-token", TEST_NIP))
                .offlineSigning(provider)
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build()) {

            KsefClientConfig config = client.config();
            assertTrue(config.offlineSigningProvider().isPresent(),
                    "With .offlineSigning(...) the config snapshot's provider must be present.");
            assertSame(provider, config.offlineSigningProvider().orElseThrow(),
                    "Provider reference must round-trip through the config snapshot unchanged.");
        }
    }
}
