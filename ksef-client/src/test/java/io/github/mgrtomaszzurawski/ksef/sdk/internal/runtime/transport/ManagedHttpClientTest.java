/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit guard for {@link ManagedHttpClient} — the transport-resilience layer
 * (ADR-036). Reproduces the production incident: a long-lived client whose
 * pooled connection was silently half-closed timed out on every call and never
 * recovered without a process restart. The managed client must rebuild on a
 * fresh connection and, for an idempotent request, recover on the same call —
 * with no business {@code RetryPolicy} involved (this layer sits below it).
 */
class ManagedHttpClientTest {

    private static final Duration TTL = Duration.ofSeconds(60);
    private static final URI ENDPOINT = URI.create("https://api.example.test/v2/security/public-key-certificates");

    private static HttpRequest getRequest() {
        return HttpRequest.newBuilder(ENDPOINT).GET().build();
    }

    private static HttpRequest postRequest() {
        return HttpRequest.newBuilder(ENDPOINT).POST(HttpRequest.BodyPublishers.noBody()).build();
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> okResponse() {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        return response;
    }

    /** A factory that hands out the given clients in order, one per rebuild. */
    private static Supplier<HttpClient> factoryOf(List<HttpClient> clients) {
        AtomicInteger index = new AtomicInteger();
        return () -> clients.get(index.getAndIncrement());
    }

    @Test
    void send_idempotentTransportFailure_rebuildsAndRecoversOnFreshConnection() throws Exception {
        // Reproduces the incident: first client's pooled connection is dead
        // (read timeout); the managed client must rebuild and the retry succeed.
        HttpClient dead = mock(HttpClient.class);
        when(dead.send(any(), any(BodyHandler.class))).thenThrow(new HttpTimeoutException("request timed out"));
        HttpClient fresh = mock(HttpClient.class);
        HttpResponse<String> ok = okResponse();
        when(fresh.send(any(), any(BodyHandler.class))).thenReturn(ok);

        ManagedHttpClient managed = new ManagedHttpClient(factoryOf(List.of(dead, fresh)), TTL);

        HttpResponse<String> result = managed.send(getRequest(), HttpResponse.BodyHandlers.ofString(), true);

        assertSame(ok, result);
        assertSame(fresh, managed.current());
        verify(dead).send(any(), any(BodyHandler.class));
        verify(fresh).send(any(), any(BodyHandler.class));
    }

    @Test
    void send_nonIdempotentTransportFailure_rebuildsButDoesNotRetry() throws Exception {
        HttpClient dead = mock(HttpClient.class);
        when(dead.send(any(), any(BodyHandler.class))).thenThrow(new IOException("connection reset"));
        HttpClient fresh = mock(HttpClient.class);

        ManagedHttpClient managed = new ManagedHttpClient(factoryOf(List.of(dead, fresh)), TTL);

        assertThrows(IOException.class,
                () -> managed.send(postRequest(), HttpResponse.BodyHandlers.ofString(), false));
        // Rebuilt so the NEXT call is healthy, but the POST was not replayed.
        assertSame(fresh, managed.current());
        verify(fresh, never()).send(any(), any(BodyHandler.class));
    }

    @Test
    void send_idleBeyondTtl_rebuildsBeforeSend() throws Exception {
        HttpClient stale = mock(HttpClient.class);
        HttpClient fresh = mock(HttpClient.class);
        HttpResponse<String> ok = okResponse();
        when(fresh.send(any(), any(BodyHandler.class))).thenReturn(ok);

        AtomicLong now = new AtomicLong(0L);
        ManagedHttpClient managed =
                new ManagedHttpClient(factoryOf(List.of(stale, fresh)), Duration.ofSeconds(30), now::get);

        now.set(Duration.ofSeconds(45).toNanos()); // idle 45s > 30s TTL

        HttpResponse<String> result = managed.send(getRequest(), HttpResponse.BodyHandlers.ofString(), true);

        assertSame(ok, result);
        assertSame(fresh, managed.current());
        verify(stale, never()).send(any(), any(BodyHandler.class));
        verify(fresh).send(any(), any(BodyHandler.class));
    }

    @Test
    void send_withinTtl_reusesSameClient() throws Exception {
        HttpClient warm = mock(HttpClient.class);
        HttpResponse<String> ok = okResponse();
        when(warm.send(any(), any(BodyHandler.class))).thenReturn(ok);
        HttpClient shouldNotBuild = mock(HttpClient.class);

        AtomicLong now = new AtomicLong(0L);
        ManagedHttpClient managed =
                new ManagedHttpClient(factoryOf(List.of(warm, shouldNotBuild)), Duration.ofSeconds(30), now::get);

        now.set(Duration.ofSeconds(10).toNanos()); // idle 10s < 30s TTL

        managed.send(getRequest(), HttpResponse.BodyHandlers.ofString(), true);

        assertSame(warm, managed.current());
        verify(warm).send(any(), any(BodyHandler.class));
    }

    @Test
    void invalidate_forcesFreshClientOnNextUse() {
        HttpClient first = mock(HttpClient.class);
        HttpClient second = mock(HttpClient.class);

        ManagedHttpClient managed = new ManagedHttpClient(factoryOf(List.of(first, second)), TTL);
        assertSame(first, managed.current());

        managed.invalidate();

        assertSame(second, managed.current());
    }

    @Test
    void idleTtlZero_disablesIdleRebuild() throws Exception {
        HttpClient warm = mock(HttpClient.class);
        HttpResponse<String> ok = okResponse();
        when(warm.send(any(), any(BodyHandler.class))).thenReturn(ok);
        HttpClient shouldNotBuild = mock(HttpClient.class);

        AtomicLong now = new AtomicLong(0L);
        ManagedHttpClient managed =
                new ManagedHttpClient(factoryOf(List.of(warm, shouldNotBuild)), Duration.ZERO, now::get);

        now.set(Duration.ofHours(1).toNanos()); // very idle, but TTL disabled

        managed.send(getRequest(), HttpResponse.BodyHandlers.ofString(), true);

        assertSame(warm, managed.current());
        verify(warm).send(any(), any(BodyHandler.class));
    }
}
