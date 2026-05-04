/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared HTTP request/response handling for domain clients.
 * Not part of the public API — used internally by SecurityClient, AuthClient, etc.
 *
 * <p>Authenticated methods automatically retry once on HTTP 401 after re-authenticating.
 * If the retry also returns 401, the {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException}
 * propagates to the caller.
 */
public final class HttpSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSupport.class);
    private static final String LOG_REQUEST = "[{}] {}";
    private static final String LOG_RESPONSE = "[{}] {} -> {} ({}ms)";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_ERROR_FORMAT_HEADER = "X-Error-Format";
    private static final String X_ERROR_FORMAT_PROBLEM_DETAILS = "problem-details";
    private static final String X_KSEF_FEATURE_HEADER = "X-KSeF-Feature";
    private static final String UPO_V4_3_FEATURE = "upo-v4-3";
    /** Substring matched against request path to detect UPO-related calls (REQ-MISC; FeaturePolicy). */
    private static final String UPO_PATH_FRAGMENT = "/upo";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * RFC 7231 §7.1.1.1 permits three date formats for HTTP-date values.
     * The first two carry an explicit zone; asctime has none and is interpreted
     * as GMT per the spec, which is why its formatter applies a default zone.
     */
    private static final java.time.format.DateTimeFormatter[] HTTP_DATE_FORMATS = {
            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME,
            java.time.format.DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", java.util.Locale.ROOT),
            new java.time.format.DateTimeFormatterBuilder()
                    .appendPattern("EEE MMM d HH:mm:ss yyyy")
                    .toFormatter(java.util.Locale.ROOT)
                    .withZone(java.time.ZoneOffset.UTC),
    };
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("^[A-Za-z0-9._\\-]+$");
    private static final String ERR_UNSAFE_PATH = "Unsafe path segment: ";

    private final HttpRuntime runtime;

    public HttpSupport(HttpRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Return the current access token, authenticating proactively if no token
     * has been issued yet. Domain clients call this before every protected
     * request so the first call after {@code KsefClient} construction does not
     * leave with {@code Authorization: Bearer null}.
     */
    public String requireToken() {
        return runtime.requireToken();
    }

    /**
     * Validate that a value is safe for use as a URL path segment.
     * Rejects values containing path separators, query strings, fragments, or traversal sequences.
     *
     * @param segment the path segment to validate
     * @return the validated segment (unchanged)
     * @throws IllegalArgumentException if the segment contains unsafe characters
     */
    public static String requireSafePathSegment(String segment) {
        if (segment == null || !SAFE_PATH_SEGMENT.matcher(segment).matches()) {
            throw new IllegalArgumentException(ERR_UNSAFE_PATH + segment);
        }
        return segment;
    }

    /**
     * Build a URI from a path relative to the base URL.
     */
    public URI uri(String path) {
        return URI.create(runtime.baseUrl() + path);
    }

    /**
     * Send a GET request and deserialize the JSON response.
     */
    public <T> T get(String path, Class<T> responseType, String operationName) {
        return runtime.retryHandler().execute(() -> {
            HttpRequest request = newGetBuilder(path).build();
            return sendAndDeserialize(request, responseType);
        }, operationName);
    }

    /**
     * Send an authenticated GET request and deserialize the JSON response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T getAuthenticated(String path, String token, Class<T> responseType, String operationName) {
        return runtime.retryHandler().execute(() -> sendAuthenticatedJson(
                bearer -> newGetBuilder(path)
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .build(),
                token, responseType), operationName);
    }

    /**
     * Send a GET request and deserialize a JSON array response.
     */
    public <T> List<T> getList(String path, TypeReference<List<T>> typeRef, String operationName) {
        return runtime.retryHandler().execute(() -> {
            HttpRequest request = newGetBuilder(path).build();
            return sendAndDeserializeList(request, typeRef);
        }, operationName);
    }

    /**
     * Send a POST request with no body and deserialize the JSON response.
     */
    public <T> T postNoBody(String path, Class<T> responseType, String operationName) {
        return runtime.retryHandler().executePost(() -> {
            HttpRequest request = newPostBuilder(path)
                    .header(ACCEPT, APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return sendAndDeserialize(request, responseType);
        }, operationName);
    }

    /**
     * Send a POST request with JSON body and deserialize the response.
     */
    public <T> T postJson(String path, Object body, Class<T> responseType, String operationName) {
        return runtime.retryHandler().executePost(() -> {
            String jsonBody = runtime.objectMapper().writeValueAsString(body);
            HttpRequest request = newPostBuilder(path)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            return sendAndDeserialize(request, responseType);
        }, operationName);
    }

    /**
     * Send an authenticated POST with JSON body and deserialize the response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T postJsonAuthenticated(String path, Object body, String token,
                                       Class<T> responseType, String operationName) {
        return runtime.retryHandler().executePost(() -> {
            String jsonBody = runtime.objectMapper().writeValueAsString(body);
            return sendAuthenticatedJson(
                    bearer -> newPostBuilder(path)
                            .header(CONTENT_TYPE, APPLICATION_JSON)
                            .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build(),
                    token, responseType);
        }, operationName);
    }

    /**
     * Send an authenticated POST with no body and deserialize the response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T postAuthenticated(String path, String token, Class<T> responseType, String operationName) {
        return runtime.retryHandler().executePost(() -> sendAuthenticatedJson(
                bearer -> newPostBuilder(path)
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                token, responseType), operationName);
    }

    /**
     * Send a POST with XML body (for XAdES signed auth requests).
     */
    public <T> T postXml(String path, String xmlBody, Class<T> responseType, String operationName) {
        return runtime.retryHandler().executePost(() -> {
            HttpRequest request = newPostBuilder(path)
                    .header(CONTENT_TYPE, APPLICATION_XML)
                    .POST(HttpRequest.BodyPublishers.ofString(xmlBody))
                    .build();
            return sendAndDeserialize(request, responseType);
        }, operationName);
    }

    /**
     * Send an authenticated POST with JSON body and expect no content (204).
     * Retries once on HTTP 401 after re-authentication.
     */
    public void postJsonAuthenticatedNoContent(String path, Object body, String token, String operationName) {
        runtime.retryHandler().runPost(() -> {
            String jsonBody = runtime.objectMapper().writeValueAsString(body);
            sendAuthenticatedNoContent(
                    bearer -> newPostBuilder(path)
                            .header(CONTENT_TYPE, APPLICATION_JSON)
                            .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build(),
                    token);
        }, operationName);
    }

    /**
     * Send a POST with JSON body and expect no content (204). No authentication.
     */
    public void postJsonNoContent(String path, Object body, String operationName) {
        runtime.retryHandler().runPost(() -> {
            String jsonBody = runtime.objectMapper().writeValueAsString(body);
            HttpRequest request = newPostBuilder(path)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            sendExpectNoContent(request);
        }, operationName);
    }

    /**
     * Send an authenticated POST with no body and expect no content (204).
     * Retries once on HTTP 401 after re-authentication.
     */
    public void postNoBodyAuthenticated(String path, String token, String operationName) {
        runtime.retryHandler().runPost(() -> sendAuthenticatedNoContent(
                bearer -> newPostBuilder(path)
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                token), operationName);
    }

    /**
     * Send an authenticated GET request and return raw bytes (for binary responses like UPO).
     * Retries once on HTTP 401 after re-authentication.
     */
    public byte[] getAuthenticatedBytes(String path, String token, String operationName) {
        return runtime.retryHandler().execute(() -> sendAuthenticatedBytes(
                bearer -> {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(uri(path))
                            .timeout(runtime.readTimeout())
                            .header(AUTHORIZATION, BEARER_PREFIX + bearer);
                    applyFeatureHeaders(builder, path);
                    return builder.GET().build();
                },
                token), operationName);
    }

    /**
     * Send an authenticated DELETE and deserialize the JSON response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T deleteAuthenticatedWithResponse(String path, String token,
                                                  Class<T> responseType, String operationName) {
        return runtime.retryHandler().execute(() -> sendAuthenticatedJson(
                bearer -> {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(uri(path))
                            .timeout(runtime.readTimeout())
                            .header(ACCEPT, APPLICATION_JSON)
                            .header(AUTHORIZATION, BEARER_PREFIX + bearer);
                    applyFeatureHeaders(builder, path);
                    return builder.DELETE().build();
                },
                token, responseType), operationName);
    }

    /**
     * Send an authenticated DELETE with no response body.
     * Retries once on HTTP 401 after re-authentication.
     */
    public void deleteAuthenticated(String path, String token, String operationName) {
        runtime.retryHandler().runPost(() -> sendAuthenticatedNoContent(
                bearer -> {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(uri(path))
                            .timeout(runtime.readTimeout())
                            .header(AUTHORIZATION, BEARER_PREFIX + bearer);
                    applyFeatureHeaders(builder, path);
                    return builder.DELETE().build();
                },
                token), operationName);
    }

    private HttpRequest.Builder newGetBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(runtime.readTimeout())
                .header(ACCEPT, APPLICATION_JSON);
        applyFeatureHeaders(builder, path);
        return builder.GET();
    }

    private HttpRequest.Builder newPostBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(runtime.readTimeout());
        applyFeatureHeaders(builder, path);
        return builder;
    }

    /**
     * Apply headers driven by {@link io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy}.
     *
     * <ul>
     *   <li>{@code X-Error-Format: problem-details} — only when
     *       {@link io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy#problemDetails()}
     *       is {@code true}. Default policy enables it.</li>
     *   <li>{@code X-KSeF-Feature: upo-v4-3} — only on UPO-related calls
     *       when {@link io.github.mgrtomaszzurawski.ksef.sdk.config.UpoVersion#V4_3} is
     *       selected. Detected from path containing {@code /upo}.</li>
     * </ul>
     */
    private void applyFeatureHeaders(HttpRequest.Builder builder, String path) {
        io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy policy = runtime.featurePolicy();
        if (policy == null) {
            // Defensive — pre-1.0.0 runtimes may have null. Apply legacy behavior.
            builder.header(X_ERROR_FORMAT_HEADER, X_ERROR_FORMAT_PROBLEM_DETAILS);
            return;
        }
        if (policy.problemDetails()) {
            builder.header(X_ERROR_FORMAT_HEADER, X_ERROR_FORMAT_PROBLEM_DETAILS);
        }
        String featureValue = featureHeaderValue(policy, path);
        if (featureValue != null) {
            builder.header(X_KSEF_FEATURE_HEADER, featureValue);
        }
    }

    private static String featureHeaderValue(
            io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy policy,
            String path) {
        if (policy.upoVersion() == io.github.mgrtomaszzurawski.ksef.sdk.config.UpoVersion.V4_3
                && isUpoPath(path)) {
            return UPO_V4_3_FEATURE;
        }
        return null;
    }

    private static boolean isUpoPath(String path) {
        return path != null && path.contains(UPO_PATH_FRAGMENT);
    }

    /**
     * Send an authenticated request expecting a JSON body. On 401, re-authenticate and retry
     * once. If re-authentication itself fails, the original 401 is propagated as
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException}.
     */
    private <T> T sendAuthenticatedJson(Function<String, HttpRequest> requestBuilder,
                                        String initialToken,
                                        Class<T> responseType) throws IOException {
        HttpRequest request = requestBuilder.apply(initialToken);
        HttpResponse<String> response = send(request);
        if (response.statusCode() == HTTP_UNAUTHORIZED && tryReauthenticate()) {
            String freshToken = runtime.sessionContext().token();
            request = requestBuilder.apply(freshToken);
            response = send(request);
        }
        return handleJsonResponse(request, response, responseType);
    }

    /**
     * Send an authenticated request expecting 204. On 401, re-authenticate and retry once.
     */
    private void sendAuthenticatedNoContent(Function<String, HttpRequest> requestBuilder,
                                            String initialToken) throws IOException {
        HttpRequest request = requestBuilder.apply(initialToken);
        HttpResponse<String> response = send(request);
        if (response.statusCode() == HTTP_UNAUTHORIZED && tryReauthenticate()) {
            String freshToken = runtime.sessionContext().token();
            request = requestBuilder.apply(freshToken);
            response = send(request);
        }
        handleNoContentResponse(request, response);
    }

    /**
     * Send an authenticated request expecting binary body. On 401, re-authenticate and retry once.
     */
    private byte[] sendAuthenticatedBytes(Function<String, HttpRequest> requestBuilder,
                                          String initialToken) throws IOException {
        HttpRequest request = requestBuilder.apply(initialToken);
        HttpResponse<byte[]> response = sendBytes(request);
        if (response.statusCode() == HTTP_UNAUTHORIZED && tryReauthenticate()) {
            String freshToken = runtime.sessionContext().token();
            request = requestBuilder.apply(freshToken);
            response = sendBytes(request);
        }
        int status = response.statusCode();
        if (status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status,
                    new String(response.body(), java.nio.charset.StandardCharsets.UTF_8),
                    parseRetryAfterSeconds(response));
        }
        return response.body();
    }

    /**
     * Parse {@code Retry-After} from a response. KSeF uses delta-seconds, but
     * this helper also tolerates an HTTP-date by returning {@code null} when
     * the value cannot be read as a non-negative integer.
     */
    private static Long parseRetryAfterSeconds(HttpResponse<?> response) {
        return response.headers().firstValue(RETRY_AFTER_HEADER)
                .flatMap(HttpSupport::parseRetryAfterValue)
                .orElse(null);
    }

    private static java.util.Optional<Long> parseRetryAfterValue(String value) {
        String trimmed = value.trim();
        try {
            long seconds = Long.parseLong(trimmed);
            return seconds < 0 ? java.util.Optional.empty() : java.util.Optional.of(seconds);
        } catch (NumberFormatException notDeltaSeconds) {
            return parseRetryAfterHttpDate(trimmed);
        }
    }

    /**
     * Parse RFC 7231 Section 7.1.1.1 HTTP-date and return the delta-seconds
     * between now and the parsed instant. Negative deltas (past dates) collapse
     * to zero. Returns empty when the value is not a valid HTTP-date in any of
     * the three RFC-permitted formats.
     */
    private static java.util.Optional<Long> parseRetryAfterHttpDate(String value) {
        for (java.time.format.DateTimeFormatter formatter : HTTP_DATE_FORMATS) {
            java.util.Optional<Long> parsed = tryParseHttpDate(value, formatter);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<Long> tryParseHttpDate(String value, java.time.format.DateTimeFormatter formatter) {
        try {
            java.time.temporal.TemporalAccessor parsed = formatter.parseBest(value,
                    java.time.ZonedDateTime::from, java.time.LocalDateTime::from);
            java.time.Instant instant = (parsed instanceof java.time.ZonedDateTime zoned)
                    ? zoned.toInstant()
                    : ((java.time.LocalDateTime) parsed).atZone(java.time.ZoneOffset.UTC).toInstant();
            long deltaSeconds = java.time.Duration.between(java.time.Instant.now(), instant).toSeconds();
            return java.util.Optional.of(Math.max(0L, deltaSeconds));
        } catch (java.time.format.DateTimeParseException notThisFormat) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Attempt to re-authenticate. Returns {@code true} on success; {@code false} if the
     * re-auth itself failed — in that case the caller propagates the original 401 to the
     * consumer instead of masking it with the re-auth failure.
     */
    private boolean tryReauthenticate() {
        try {
            runtime.reauthenticate();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private <T> T sendAndDeserialize(HttpRequest request, Class<T> responseType) throws IOException {
        HttpResponse<String> response = send(request);
        return handleJsonResponse(request, response, responseType);
    }

    private <T> T handleJsonResponse(HttpRequest request, HttpResponse<String> response,
                                     Class<T> responseType) throws IOException {
        int status = response.statusCode();
        if (status != HTTP_OK && status != HTTP_CREATED && status != HTTP_ACCEPTED) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body(),
                    parseRetryAfterSeconds(response));
        }
        return deserialize(response.body(), responseType);
    }

    private <T> List<T> sendAndDeserializeList(HttpRequest request, TypeReference<List<T>> typeRef) throws IOException {
        HttpResponse<String> response = send(request);
        int status = response.statusCode();
        if (status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body(),
                    parseRetryAfterSeconds(response));
        }
        return runtime.objectMapper().readValue(response.body(), typeRef);
    }

    private void sendExpectNoContent(HttpRequest request) throws IOException {
        HttpResponse<String> response = send(request);
        handleNoContentResponse(request, response);
    }

    private void handleNoContentResponse(HttpRequest request, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status != HTTP_NO_CONTENT && status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body(),
                    parseRetryAfterSeconds(response));
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        return sendWithLogging(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendBytes(HttpRequest request) throws IOException {
        return sendWithLogging(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private <T> HttpResponse<T> sendWithLogging(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException {
        long start = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(LOG_REQUEST, request.method(), request.uri());
        }
        try {
            HttpResponse<T> response = runtime.httpClient().send(request, bodyHandler);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LOG_RESPONSE, request.method(), request.uri(),
                        response.statusCode(), System.currentTimeMillis() - start);
                logResponseBodyAtTrace(response.body());
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(request.method() + " " + request.uri() + " interrupted", exception);
        }
    }

    /**
     * Trace-level diagnostic: dumps the raw response body so a wire-shape
     * mismatch can be diagnosed against the live KSeF server. Only emitted
     * when the {@code HttpSupport} logger is at TRACE.
     */
    private static void logResponseBodyAtTrace(Object body) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        if (body instanceof byte[] bodyBytes) {
            LOGGER.trace("HTTP response body ({} bytes): {}", bodyBytes.length,
                    new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8));
        } else if (body instanceof String bodyStr) {
            LOGGER.trace("HTTP response body: {}", bodyStr);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType) throws IOException {
        return runtime.objectMapper().readValue(body, responseType);
    }

}
