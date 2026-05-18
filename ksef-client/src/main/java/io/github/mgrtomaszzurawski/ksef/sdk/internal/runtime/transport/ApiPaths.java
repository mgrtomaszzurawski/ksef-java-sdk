/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

/**
 * Centralised KSeF REST API path prefixes (ADR-014).
 *
 * <p>Each domain client builds its concrete paths from the appropriate base
 * constant rather than duplicating the domain segment in every
 * {@code PATH_*} constant.
 *
 * <p>The API version segment ({@code /v2}) is part of the server URL
 * configured via {@code KsefEnvironment} — these path constants do
 * <strong>not</strong> repeat it.
 *
 * <p>Internal — consumers use the high-level SDK methods rather than
 * composing URLs by hand.
 *
 * @since 1.0.0
 */
public final class ApiPaths {

    private static final String SEPARATOR = "/";

    public static final String AUTH = "/auth";
    public static final String CERTIFICATES = "/certificates";
    public static final String INVOICES = "/invoices";
    public static final String LIMITS = "/limits";
    public static final String PEPPOL = "/peppol";
    public static final String PERMISSIONS = "/permissions";
    public static final String RATE_LIMITS = "/rate-limits";
    public static final String SECURITY = "/security";
    public static final String SESSIONS = "/sessions";
    public static final String TESTDATA = "/testdata";
    public static final String TOKENS = "/tokens";

    private ApiPaths() { }

    /**
     * Build a sub-path by appending segments to a base path with the URL path separator.
     * Each segment is appended verbatim — caller is responsible for any encoding.
     *
     * <p>Example: {@code subPath(SESSIONS, ref, "invoices")} →
     * {@code "/sessions/&lt;ref&gt;/invoices"}.
     */
    public static String subPath(String base, String... segments) {
        StringBuilder path = new StringBuilder(base);
        for (String segment : segments) {
            path.append(SEPARATOR).append(segment);
        }
        return path.toString();
    }
}
