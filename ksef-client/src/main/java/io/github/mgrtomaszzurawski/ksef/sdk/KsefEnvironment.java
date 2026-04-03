/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

/**
 * KSeF API environment configuration.
 */
public final class KsefEnvironment {

    private static final String TEST_URL = "https://api-test.ksef.mf.gov.pl/v2";
    private static final String PREPROD_URL = "https://api-preprod.ksef.mf.gov.pl/v2";
    private static final String PROD_URL = "https://api.ksef.mf.gov.pl/v2";
    private static final String NULL_URL_MESSAGE = "baseUrl must not be null";

    public static final KsefEnvironment TEST = new KsefEnvironment(TEST_URL);
    public static final KsefEnvironment PREPROD = new KsefEnvironment(PREPROD_URL);
    public static final KsefEnvironment PROD = new KsefEnvironment(PROD_URL);

    private final String baseUrl;

    private KsefEnvironment(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Create a custom environment pointing to an arbitrary URL.
     * Use for proxies, mock servers, or non-standard deployments.
     */
    public static KsefEnvironment custom(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException(NULL_URL_MESSAGE);
        }
        return new KsefEnvironment(baseUrl);
    }
}
