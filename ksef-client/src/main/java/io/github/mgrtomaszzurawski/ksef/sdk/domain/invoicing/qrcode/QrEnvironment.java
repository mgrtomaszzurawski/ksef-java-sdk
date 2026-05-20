/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import java.util.Objects;

/**
 * KSeF QR-code base URLs per environment, as defined in
 * <a href="https://github.com/CIRFMF/ksef-docs/blob/main/kody-qr.md">kody-qr.md</a>.
 *
 * <p>The QR hosts are independent of the API hosts ({@code api-test}, {@code api-demo},
 * {@code api}) — verification links are served from {@code qr-test}, {@code qr-demo},
 * and {@code qr} respectively.
 *
 * @since 0.1.0
 */
public enum QrEnvironment {
    /** Test environment ({@code https://qr-test.ksef.mf.gov.pl}). */
    TEST("https://qr-test.ksef.mf.gov.pl"),
    /** Demo environment ({@code https://qr-demo.ksef.mf.gov.pl}). */
    DEMO("https://qr-demo.ksef.mf.gov.pl"),
    /** Production environment ({@code https://qr.ksef.mf.gov.pl}). */
    PROD("https://qr.ksef.mf.gov.pl");

    private final String baseUrl;

    QrEnvironment(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Base URL for the environment, no trailing slash. */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Map a {@link KsefEnvironment} to its corresponding QR
     * environment by API base URL inspection. The QR hosts are
     * independent of the API hosts but the SDK's three predefined
     * {@code KsefEnvironment} constants align 1:1 with TEST / DEMO /
     * PROD QR hosts.
     *
     * <p>Custom environments fall back to {@link #TEST} so smoke tests
     * against mock servers do not crash; consumers using a true custom
     * KSeF deployment should construct their own QR URLs externally.
     *
     * @param environment non-null KSeF API environment
     * @return matching QR environment
     * @throws NullPointerException if {@code environment} is null
     */
    public static QrEnvironment fromKsefEnvironment(KsefEnvironment environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        String baseUrl = environment.baseUrl();
        if (baseUrl.contains("api-test.")) {
            return TEST;
        }
        if (baseUrl.contains("api-demo.")) {
            return DEMO;
        }
        if (baseUrl.contains("api.ksef.")) {
            return PROD;
        }
        // Custom (mock / proxy) — use TEST by default. Documented:
        // consumers running against custom infra should not rely on the
        // SDK's KOD I QR generation pointing to a real verification host.
        return TEST;
    }
}
