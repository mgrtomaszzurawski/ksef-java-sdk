/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

/**
 * KSeF QR-code base URLs per environment, as defined in
 * <a href="https://github.com/CIRFMF/ksef-docs/blob/main/kody-qr.md">kody-qr.md</a>.
 *
 * <p>The QR hosts are independent of the API hosts ({@code api-test}, {@code api-demo},
 * {@code api}) — verification links are served from {@code qr-test}, {@code qr-demo},
 * and {@code qr} respectively.
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
}
