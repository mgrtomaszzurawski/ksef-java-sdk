/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.transport;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.auth.AuthClient;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Shared HTTP request/response handling for domain clients.
 * Not part of the public API — used internally by SecurityClient, AuthClient, etc.
 *
 * <p>Authenticated methods automatically retry once on HTTP 401 after re-authenticating.
 * If the retry also returns 401, the {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefAuthException}
 * propagates to the caller.
 */
public final class HttpSupport {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("^[A-Za-z0-9._\\-]+$");
    private static final String ERR_UNSAFE_PATH = "Unsafe path segment: ";

    private final KsefClient ksef;

    public HttpSupport(KsefClient ksef) {
        this.ksef = ksef;
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
        return URI.create(ksef.environment().baseUrl() + path);
    }

    /**
     * Send a GET request and deserialize the JSON response.
     */
    public <T> T get(String path, Class<T> responseType, String operationName) {
        return ksef.retryHandler().execute(() -> {
            HttpRequest request = newGetBuilder(path).build();
            return sendAndDeserialize(request, responseType);
        }, operationName);
    }

    /**
     * Send an authenticated GET request and deserialize the JSON response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T getAuthenticated(String path, String token, Class<T> responseType, String operationName) {
        return ksef.retryHandler().execute(() -> sendAuthenticatedJson(
                bearer -> newGetBuilder(path)
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .build(),
                token, responseType), operationName);
    }

    /**
     * Send a GET request and deserialize a JSON array response.
     */
    public <T> List<T> getList(String path, TypeReference<List<T>> typeRef, String operationName) {
        return ksef.retryHandler().execute(() -> {
            HttpRequest request = newGetBuilder(path).build();
            return sendAndDeserializeList(request, typeRef);
        }, operationName);
    }

    /**
     * Send a POST request with no body and deserialize the JSON response.
     */
    public <T> T postNoBody(String path, Class<T> responseType, String operationName) {
        return ksef.retryHandler().executePost(() -> {
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
        return ksef.retryHandler().executePost(() -> {
            String jsonBody = ksef.objectMapper().writeValueAsString(body);
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
        return ksef.retryHandler().executePost(() -> {
            String jsonBody = ksef.objectMapper().writeValueAsString(body);
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
        return ksef.retryHandler().executePost(() -> sendAuthenticatedJson(
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
        return ksef.retryHandler().executePost(() -> {
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
        ksef.retryHandler().run(() -> {
            String jsonBody = ksef.objectMapper().writeValueAsString(body);
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
        ksef.retryHandler().run(() -> {
            String jsonBody = ksef.objectMapper().writeValueAsString(body);
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
        ksef.retryHandler().run(() -> sendAuthenticatedNoContent(
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
        return ksef.retryHandler().execute(() -> sendAuthenticatedBytes(
                bearer -> HttpRequest.newBuilder()
                        .uri(uri(path))
                        .timeout(ksef.readTimeout())
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .GET()
                        .build(),
                token), operationName);
    }

    /**
     * Send an authenticated DELETE and deserialize the JSON response.
     * Retries once on HTTP 401 after re-authentication.
     */
    public <T> T deleteAuthenticatedWithResponse(String path, String token,
                                                  Class<T> responseType, String operationName) {
        return ksef.retryHandler().execute(() -> sendAuthenticatedJson(
                bearer -> HttpRequest.newBuilder()
                        .uri(uri(path))
                        .timeout(ksef.readTimeout())
                        .header(ACCEPT, APPLICATION_JSON)
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .DELETE()
                        .build(),
                token, responseType), operationName);
    }

    /**
     * Send an authenticated DELETE with no response body.
     * Retries once on HTTP 401 after re-authentication.
     */
    public void deleteAuthenticated(String path, String token, String operationName) {
        ksef.retryHandler().run(() -> sendAuthenticatedNoContent(
                bearer -> HttpRequest.newBuilder()
                        .uri(uri(path))
                        .timeout(ksef.readTimeout())
                        .header(AUTHORIZATION, BEARER_PREFIX + bearer)
                        .DELETE()
                        .build(),
                token), operationName);
    }

    private HttpRequest.Builder newGetBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(ksef.readTimeout())
                .header(ACCEPT, APPLICATION_JSON)
                .GET();
    }

    private HttpRequest.Builder newPostBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(ksef.readTimeout());
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
            String freshToken = ksef.sessionContext().token();
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
            String freshToken = ksef.sessionContext().token();
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
            String freshToken = ksef.sessionContext().token();
            request = requestBuilder.apply(freshToken);
            response = sendBytes(request);
        }
        int status = response.statusCode();
        if (status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status,
                    new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return response.body();
    }

    /**
     * Attempt to re-authenticate. Returns {@code true} on success; {@code false} if the
     * re-auth itself failed — in that case the caller propagates the original 401 to the
     * consumer instead of masking it with the re-auth failure.
     */
    private boolean tryReauthenticate() {
        try {
            ksef.reauthenticate();
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
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body());
        }
        return deserialize(response.body(), responseType);
    }

    private <T> List<T> sendAndDeserializeList(HttpRequest request, TypeReference<List<T>> typeRef) throws IOException {
        HttpResponse<String> response = send(request);
        int status = response.statusCode();
        if (status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body());
        }
        return ksef.objectMapper().readValue(response.body(), typeRef);
    }

    private void sendExpectNoContent(HttpRequest request) throws IOException {
        HttpResponse<String> response = send(request);
        handleNoContentResponse(request, response);
    }

    private void handleNoContentResponse(HttpRequest request, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status != HTTP_NO_CONTENT && status != HTTP_OK) {
            throw KsefException.of(request.method() + " " + request.uri(), null, status, response.body());
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return ksef.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(request.method() + " " + request.uri() + " interrupted", exception);
        }
    }

    private HttpResponse<byte[]> sendBytes(HttpRequest request) throws IOException {
        try {
            return ksef.httpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(request.method() + " " + request.uri() + " interrupted", exception);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType) throws IOException {
        return ksef.objectMapper().readValue(body, responseType);
    }

}
