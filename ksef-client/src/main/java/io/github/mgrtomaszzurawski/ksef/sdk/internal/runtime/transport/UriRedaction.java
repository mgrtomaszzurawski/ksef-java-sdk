/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import java.net.URI;

/**
 * URI redaction helper for log/error messages. KSeF returns presigned
 * storage URLs (Azure Blob, S3-style) for both export download and batch
 * part upload, with credential-bearing query parameters such as {@code sig},
 * {@code skoid}, {@code skt}, {@code se}. Those URLs MUST be scrubbed before
 * appearing in logs or thrown exception messages — DEBUG logs are commonly
 * enabled during support incidents and routinely shipped to centralised
 * log stores.
 *
 * <p>Codex round-9 fresh review H1: {@code KsefBatchSession} previously
 * logged the full upload URL at DEBUG; export error messages already
 * redacted via the inline helper this class generalises.
 *
 * @since 1.0.0
 */
public final class UriRedaction {

    private static final String SCHEME_SEPARATOR = "://";
    private static final String QUERY_SEPARATOR = "?";
    private static final String REDACTED_PLACEHOLDER = "<redacted>";

    private UriRedaction() { }

    /**
     * Strip the query string from {@code url} and replace it with
     * {@code <redacted>}. Returns the original URL when no query string is
     * present.
     *
     * @param url the URL to redact (must not be {@code null})
     * @return the redacted URL string
     */
    public static String redactQuery(URI url) {
        if (url.getRawQuery() == null) {
            return url.toString();
        }
        String scheme = url.getScheme();
        String authority = url.getRawAuthority();
        String path = url.getRawPath();
        return scheme + SCHEME_SEPARATOR + authority + (path == null ? "" : path)
                + QUERY_SEPARATOR + REDACTED_PLACEHOLDER;
    }
}
