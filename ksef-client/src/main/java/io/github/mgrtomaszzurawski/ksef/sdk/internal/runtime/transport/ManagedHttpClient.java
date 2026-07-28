/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the lifetime of the shared {@link HttpClient} for a {@code KsefClient} and
 * makes a long-lived client survive a silently dropped (half-open)
 * keep-alive / HTTP-2 connection without a process restart.
 *
 * <p>Background: {@code java.net.http.HttpClient} exposes no per-connection
 * eviction, no validate-after-inactivity, and no per-client idle timeout, so the
 * only lever to guarantee a fresh connection is to build a new client (a new
 * connection pool). A single silently half-closed connection would otherwise be
 * reused forever — every request timing out until the process is restarted (see
 * the transport-resilience decision, ADR-036). This type centralises the fix:
 *
 * <ul>
 *   <li><b>Idle TTL</b> — before a send, if the client has been idle longer than
 *       the configured TTL it is rebuilt, so a connection the edge/NAT may have
 *       silently dropped during an idle window is never reused. A soft
 *       optimisation; {@link Duration#ZERO} (or negative) disables it. Rebuilds
 *       are kept infrequent because a new {@code HttpClient} allocates a selector
 *       thread.</li>
 *   <li><b>Transport-failure recovery</b> — an {@link IOException} on send
 *       (timeout, connection reset) rebuilds the client on a fresh pool so the
 *       <em>next</em> call is healthy; for an idempotent request the send is also
 *       retried once on the new client. This is the actual guarantee, and it is
 *       independent of the business {@code RetryPolicy}.</li>
 *   <li><b>{@link #invalidate()}</b> — forces a rebuild on next use; backs the
 *       public {@code KsefClient.reconnect()} hook.</li>
 * </ul>
 *
 * <p>Thread-safe: the live client is held in an {@link AtomicReference} and a
 * rebuild only swaps in a fresh client when the caller still observed the failing
 * instance, so concurrent failures on the same dead client do not stampede
 * rebuilds.
 *
 * @since 0.1.2
 */
public final class ManagedHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedHttpClient.class);
    private static final String ERR_NULL_FACTORY = "factory must not be null";
    private static final String ERR_NULL_TTL = "idleTtl must not be null";
    private static final String ERR_NULL_CLOCK = "nanoClock must not be null";
    private static final String ERR_FACTORY_NULL_CLIENT = "factory produced a null HttpClient";
    private static final String LOG_IDLE_REBUILD =
            "HTTP client idle {}ms exceeds TTL {}ms — rebuilding transport before request";
    private static final String LOG_RETRY_REBUILD =
            "Transport failure on idempotent {} — rebuilt HTTP client and retried on a fresh connection";
    private static final String LOG_NORETRY_REBUILD =
            "Transport failure on {} — rebuilt HTTP client; non-idempotent request not retried";
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Supplier<HttpClient> factory;
    private final long idleTtlNanos;
    private final LongSupplier nanoClock;
    private final AtomicReference<HttpClient> clientRef;
    private volatile long lastUsedNanos;

    public ManagedHttpClient(Supplier<HttpClient> factory, Duration idleTtl) {
        this(factory, idleTtl, System::nanoTime);
    }

    ManagedHttpClient(Supplier<HttpClient> factory, Duration idleTtl, LongSupplier nanoClock) {
        this.factory = Objects.requireNonNull(factory, ERR_NULL_FACTORY);
        this.idleTtlNanos = Objects.requireNonNull(idleTtl, ERR_NULL_TTL).toNanos();
        this.nanoClock = Objects.requireNonNull(nanoClock, ERR_NULL_CLOCK);
        this.clientRef = new AtomicReference<>(Objects.requireNonNull(factory.get(), ERR_FACTORY_NULL_CLIENT));
        this.lastUsedNanos = nanoClock.getAsLong();
    }

    /**
     * The current live client. Callers that cache this reference will not observe
     * a later rebuild — prefer {@link #send} for the managed path, or call this
     * fresh at each use.
     */
    public HttpClient current() {
        return clientRef.get();
    }

    /** Force a fresh transport on the next use. Backs {@code KsefClient.reconnect()}. */
    public void invalidate() {
        rebuild(clientRef.get());
    }

    /**
     * Send through the managed client. Rebuilds a stale (idle) client before the
     * request, and on a transport {@link IOException} rebuilds and — for an
     * idempotent request — retries once on the fresh client.
     *
     * @param request the request to send
     * @param bodyHandler the response body handler
     * @param safeToReplay whether the request is a safe (side-effect-free)
     *     method that may be replayed after a transport failure — GET/HEAD. A
     *     request that is not safe to replay is not retried, but the client is
     *     still rebuilt so the next call is healthy
     */
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler, boolean safeToReplay)
            throws IOException, InterruptedException {
        rebuildIfIdle();
        HttpClient client = clientRef.get();
        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            lastUsedNanos = nanoClock.getAsLong();
            return response;
        } catch (IOException transportFailure) {
            rebuild(client);
            if (!safeToReplay) {
                LOGGER.warn(LOG_NORETRY_REBUILD, request.method());
                throw transportFailure;
            }
            LOGGER.warn(LOG_RETRY_REBUILD, request.method());
            HttpResponse<T> response = clientRef.get().send(request, bodyHandler);
            lastUsedNanos = nanoClock.getAsLong();
            return response;
        }
    }

    private void rebuildIfIdle() {
        if (idleTtlNanos <= 0) {
            return;
        }
        long idleNanos = nanoClock.getAsLong() - lastUsedNanos;
        if (idleNanos > idleTtlNanos) {
            LOGGER.debug(LOG_IDLE_REBUILD, idleNanos / NANOS_PER_MILLI, idleTtlNanos / NANOS_PER_MILLI);
            rebuild(clientRef.get());
        }
    }

    // Identity comparison is intentional: the check mirrors the AtomicReference
    // compareAndSet below (which is reference-identity), and HttpClient has no
    // meaningful equals — we ask "is the live client still the instance that failed?".
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void rebuild(HttpClient known) {
        // Skip the allocation entirely if another thread already rebuilt past
        // `known` — a new HttpClient spawns a selector thread, so this collapses
        // the common case of N concurrent failures on one dead client into a
        // single rebuild instead of N. A residual race (two threads both pass
        // this check) is still safe: only one compareAndSet wins.
        if (clientRef.get() != known) {
            return;
        }
        HttpClient fresh = Objects.requireNonNull(factory.get(), ERR_FACTORY_NULL_CLIENT);
        // Only the thread that still observes `known` swaps it in; a concurrent
        // rebuild on the same dead client leaves `fresh` unreferenced. HttpClient
        // is not AutoCloseable on JDK 17, so the loser is left for GC.
        if (clientRef.compareAndSet(known, fresh)) {
            lastUsedNanos = nanoClock.getAsLong();
        }
    }
}
