/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

/**
 * Centralised KSeF REST API path prefixes.
 *
 * <p>Every endpoint path lives under {@code /api/v2/}. Each domain client
 * builds its concrete paths from the appropriate base constant rather than
 * duplicating the version + domain segment in every {@code PATH_*} constant.
 *
 * <p>Changing the API major version (e.g. {@code v2 -> v3}) only touches
 * {@link #API_BASE}.
 */
public final class ApiPaths {

    /** Common prefix for every KSeF REST endpoint. */
    public static final String API_BASE = "/api/v2";

    public static final String AUTH = API_BASE + "/auth";
    public static final String CERTIFICATES = API_BASE + "/certificates";
    public static final String INVOICES = API_BASE + "/invoices";
    public static final String LIMITS = API_BASE + "/limits";
    public static final String PEPPOL = API_BASE + "/peppol";
    public static final String PERMISSIONS = API_BASE + "/permissions";
    public static final String RATE_LIMITS = API_BASE + "/rate-limits";
    public static final String SECURITY = API_BASE + "/security";
    public static final String SESSIONS = API_BASE + "/sessions";
    public static final String TESTDATA = API_BASE + "/testdata";
    public static final String TOKENS = API_BASE + "/tokens";

    private ApiPaths() { }

    /**
     * Build a sub-path by appending segments to a base path with the URL path separator.
     * Each segment is appended verbatim — caller is responsible for any encoding.
     *
     * <p>Example: {@code subPath(SESSIONS, ref, "invoices")} →
     * {@code "/api/v2/sessions/&lt;ref&gt;/invoices"}.
     */
    public static String subPath(String base, String... segments) {
        StringBuilder sb = new StringBuilder(base);
        for (String segment : segments) {
            sb.append(SEPARATOR).append(segment);
        }
        return sb.toString();
    }

    private static final String SEPARATOR = "/";
}

