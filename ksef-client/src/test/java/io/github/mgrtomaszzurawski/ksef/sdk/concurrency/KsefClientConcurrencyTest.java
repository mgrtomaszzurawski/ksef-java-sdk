/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.concurrency;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codex 2026-05-05 #12 — `KsefClient` is documented as thread-safe via
 * {@code synchronized} on its public methods. This test exercises the
 * synchronisation under contention by issuing many concurrent
 * read-side calls through one client instance and asserting all
 * complete without deadlock or visible state corruption.
 *
 * <p>This is a smoke-level proof, not a stress test for production
 * load. The point is to fail loudly if a future refactor drops the
 * {@code synchronized} keyword on a method that mutates shared
 * state.
 *
 * @since 1.0.0
 */
@WireMockTest
class KsefClientConcurrencyTest {

    private static final int THREAD_COUNT = 8;
    private static final int CALLS_PER_THREAD = 25;
    private static final long EXECUTOR_TIMEOUT_SECONDS = 60L;
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;
    private static final String TOKENS_PATH = "/v2/tokens";
    private static final String EMPTY_TOKEN_LIST = """
            {"queryTokens": [], "continuationToken": null}
            """;

    @Test
    void parallelReadOnlyCalls_completeWithoutDeadlockOrCorruption(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — every read goes through one stub returning an empty list
        stubFor(get(urlPathEqualTo(TOKENS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(EMPTY_TOKEN_LIST)));

        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            try {
                AtomicInteger errors = new AtomicInteger();
                List<Future<?>> futures = new ArrayList<>();

                for (int thread = 0; thread < THREAD_COUNT; thread++) {
                    futures.add(executor.submit(() -> {
                        for (int call = 0; call < CALLS_PER_THREAD; call++) {
                            try {
                                TokenList result = client.tokens().list(TokenQueryBuilder.create().build());
                                if (result.tokens() == null) {
                                    errors.incrementAndGet();
                                }
                            } catch (RuntimeException ex) {
                                errors.incrementAndGet();
                            }
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    future.get(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }

                assertEquals(0, errors.get(),
                        "no thread should have observed null tokens or thrown");
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "executor should terminate within "
                                + EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS + "s");
            }
        }
    }
}
