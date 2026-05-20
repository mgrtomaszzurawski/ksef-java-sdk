//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Configure FeaturePolicy on KsefClient — opt into RFC 7807 Problem
 *   Details error bodies, request a specific UPO schema version, and
 *   send the X-KSeF-Feature: enforce-xades-compliance header on the
 *   XAdES authentication paths (api-changelog v2.1.1 early-adopt).
 *
 * Side effects on KSeF: none beyond a regular authenticated client.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.UpoVersion;

public final class ConfigureFeaturePolicy {

    private ConfigureFeaturePolicy() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        FeaturePolicy features = FeaturePolicy.builder()
                .upoVersion(UpoVersion.DEFAULT)
                .problemDetails(true)
                .enforceXadesCompliance(true)
                .build();

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .features(features)
                .build()) {
            System.out.println("Client configured with features: " + client.config().featurePolicy());
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required");
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String name) {
        if (name == null || name.isBlank() || "TEST".equalsIgnoreCase(name)) {
            return KsefEnvironment.TEST;
        }
        return switch (name.toUpperCase()) {
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            case "PREPROD" -> KsefEnvironment.PREPROD;
            default -> KsefEnvironment.TEST;
        };
    }
}
