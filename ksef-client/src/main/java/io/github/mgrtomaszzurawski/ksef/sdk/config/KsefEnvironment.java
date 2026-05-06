/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;
/**
 * KSeF API environment configuration.
 *
 * @since 1.0.0
 */
public final class KsefEnvironment {

    private static final String TEST_URL = "https://api-test.ksef.mf.gov.pl/v2";
    private static final String DEMO_URL = "https://api-demo.ksef.mf.gov.pl/v2";
    private static final String PREPROD_URL = "https://api-preprod.ksef.mf.gov.pl/v2";
    private static final String PROD_URL = "https://api.ksef.mf.gov.pl/v2";
    private static final String NULL_URL_MESSAGE = "baseUrl must not be null";
    private static final String INVALID_SCHEME_MESSAGE = "baseUrl must start with http:// or https://";
    private static final String HTTPS_SCHEME = "https://";
    private static final String HTTP_SCHEME = "http://";

    /** TEST environment — full integration playground; accepts FA(2) and FA(3). */
    public static final KsefEnvironment TEST = new KsefEnvironment(TEST_URL);
    /** DEMO environment — pre-production preview; rejects FA(2). */
    public static final KsefEnvironment DEMO = new KsefEnvironment(DEMO_URL);
    /**
     * PREPROD host (api-preprod). Not listed in
     * {@code ksef-docs/srodowiska.md} (which documents only TEST, DEMO,
     * and PROD). Provided for completeness against historical KSeF
     * deployments — the host may not be reachable at all times. Prefer
     * {@link #DEMO} for pre-production validation.
     */
    public static final KsefEnvironment PREPROD = new KsefEnvironment(PREPROD_URL);
    /** PROD environment — production KSeF; rejects FA(2). */
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
        if (!baseUrl.startsWith(HTTPS_SCHEME) && !baseUrl.startsWith(HTTP_SCHEME)) {
            throw new IllegalArgumentException(INVALID_SCHEME_MESSAGE);
        }
        return new KsefEnvironment(baseUrl);
    }
}
