/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.IdentifierMasking;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * URI redaction helper for log/error messages. KSeF returns presigned
 * storage URLs (Azure Blob, S3-style) for both export download and batch
 * part upload, with credential-bearing query parameters such as {@code sig},
 * {@code skoid}, {@code skt}, {@code se}. Those URLs MUST be scrubbed before
 * appearing in logs or thrown exception messages — DEBUG logs are commonly
 * enabled during support incidents and routinely shipped to centralised
 * log stores.
 *
 * <p>Several KSeF API paths embed the seller NIP (10-digit Polish tax ID)
 * in a path segment — e.g. {@code /v2/invoices/ksef/{ksefNumber}} where
 * {@code ksefNumber} starts with the NIP, or
 * {@code /v2/sessions/{ref}/invoices/ksef/{ksefNumber}/upo}. NIP is
 * RODO-protected for sole proprietors and should not appear in DEBUG
 * logs in plaintext. {@link #redactNipSegments(URI)} masks any 10-digit
 * decimal path segment (or NIP-prefixed segment such as a 35-character
 * KSeF number) via {@link IdentifierMasking#maskTail(String)}.
 *
 * @since 1.0.0
 */
public final class UriRedaction {

    private static final String SCHEME_SEPARATOR = "://";
    private static final String QUERY_SEPARATOR = "?";
    private static final String REDACTED_PLACEHOLDER = "<redacted>";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern NIP_PREFIX_SEGMENT = Pattern.compile("^\\d{10}[-A-Z0-9]*$");

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

    /**
     * Redact NIP-bearing path segments and the query string. Path
     * segments that are pure 10-digit decimal (a NIP), or that match
     * the NIP-prefixed KSeF number shape ({@code 10-digit NIP +
     * suffix}), have all but the last four characters masked. Query
     * string (if present) is scrubbed as in {@link #redactQuery(URI)}.
     *
     * @param url the URL to redact (must not be {@code null})
     * @return the redacted URL string
     */
    public static String redactNipSegments(URI url) {
        String authority = url.getRawAuthority();
        String path = url.getRawPath();
        StringBuilder rebuilt = new StringBuilder();
        rebuilt.append(url.getScheme()).append(SCHEME_SEPARATOR);
        if (authority != null) {
            rebuilt.append(authority);
        }
        if (path != null && !path.isEmpty()) {
            String[] segments = path.split(PATH_SEPARATOR, -1);
            for (int index = 0; index < segments.length; index++) {
                if (index > 0) {
                    rebuilt.append(PATH_SEPARATOR);
                }
                rebuilt.append(maskIfNipBearing(segments[index]));
            }
        }
        if (url.getRawQuery() != null) {
            rebuilt.append(QUERY_SEPARATOR).append(REDACTED_PLACEHOLDER);
        }
        return rebuilt.toString();
    }

    private static String maskIfNipBearing(String segment) {
        if (segment.isEmpty() || !NIP_PREFIX_SEGMENT.matcher(segment).matches()) {
            return segment;
        }
        return IdentifierMasking.maskTail(segment);
    }
}
