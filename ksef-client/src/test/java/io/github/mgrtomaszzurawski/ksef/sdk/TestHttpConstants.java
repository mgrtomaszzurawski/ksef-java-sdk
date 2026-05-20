/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

/**
 * Shared HTTP-protocol constants for test classes.
 *
 * <p>Header names, prefixes, content types, and status codes are protocol
 * invariants — extracting them to a single source removes per-class string
 * duplication and prevents typos like {@code "Authorisation"} or
 * {@code "Bearer"} (no trailing space).
 */
public final class TestHttpConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";

    public static final int HTTP_OK = 200;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_SESSION_BUSY = 415;
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_BAD_GATEWAY = 502;

    private TestHttpConstants() { }
}
