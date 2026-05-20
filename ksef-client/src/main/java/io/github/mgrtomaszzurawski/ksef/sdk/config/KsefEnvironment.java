/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;
/**
 * KSeF API environment configuration.
 *
 * @since 0.1.0
 */
public final class KsefEnvironment {

    private static final String TEST_URL = "https://api-test.ksef.mf.gov.pl/v2";
    private static final String DEMO_URL = "https://api-demo.ksef.mf.gov.pl/v2";
    private static final String PROD_URL = "https://api.ksef.mf.gov.pl/v2";
    private static final String NULL_URL_MESSAGE = "baseUrl must not be null";
    private static final String INVALID_SCHEME_MESSAGE = "baseUrl scheme must be http or https (was: ";
    private static final String INSECURE_HTTP_MESSAGE =
            "baseUrl uses plain http but host is not loopback — refusing for safety. "
                    + "Plain http is only allowed when host is localhost, 127.0.0.1, or ::1. "
                    + "URL: ";
    private static final String INVALID_URL_MESSAGE = "baseUrl is not a parseable URL: ";
    private static final String MISSING_HOST_MESSAGE = "baseUrl must contain a non-empty host: ";
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String LOOPBACK_LOCALHOST = "localhost";
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String LOOPBACK_V4 = "127.0.0.1";
    /**
     * IPv6 loopback as returned by {@code URI.getHost()}. Java's URI host
     * accessor returns the bracketed form for IPv6 literals (per
     * RFC 3986 §3.2.2), so the bare {@code ::1} string is never returned —
     * the comparison must include the brackets.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String LOOPBACK_V6 = "[::1]";

    /** TEST environment — full integration playground; accepts FA(2) and FA(3). */
    public static final KsefEnvironment TEST = new KsefEnvironment(TEST_URL);
    /** DEMO environment — pre-production preview; rejects FA(2). */
    public static final KsefEnvironment DEMO = new KsefEnvironment(DEMO_URL);
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
     *
     * <p>HTTPS is required for non-loopback hosts. Plain HTTP is accepted
     * only when the host is {@code localhost}, {@code 127.0.0.1}, or
     * {@code ::1} — to keep WireMock and local dev workflows working
     * without inviting accidental plaintext in cloud / production setups.
     */
    public static KsefEnvironment custom(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException(NULL_URL_MESSAGE);
        }
        java.net.URI parsed;
        try {
            parsed = java.net.URI.create(baseUrl).parseServerAuthority();
        } catch (java.net.URISyntaxException | IllegalArgumentException malformed) {
            throw new IllegalArgumentException(INVALID_URL_MESSAGE + baseUrl, malformed);
        }
        String scheme = parsed.getScheme();
        if (!SCHEME_HTTP.equalsIgnoreCase(scheme) && !SCHEME_HTTPS.equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(INVALID_SCHEME_MESSAGE + scheme + ")");
        }
        if (parsed.getHost() == null || parsed.getHost().isBlank()) {
            throw new IllegalArgumentException(MISSING_HOST_MESSAGE + baseUrl);
        }
        if (SCHEME_HTTP.equalsIgnoreCase(scheme) && !isLoopback(parsed.getHost())) {
            throw new IllegalArgumentException(INSECURE_HTTP_MESSAGE + baseUrl);
        }
        return new KsefEnvironment(baseUrl);
    }

    private static boolean isLoopback(String host) {
        return LOOPBACK_LOCALHOST.equalsIgnoreCase(host)
                || LOOPBACK_V4.equals(host)
                || LOOPBACK_V6.equals(host);
    }
}
