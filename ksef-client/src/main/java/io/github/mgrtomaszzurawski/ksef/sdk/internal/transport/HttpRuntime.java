/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.auth.SessionContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Narrow runtime contract that {@link HttpSupport} needs from its host.
 *
 * <p>Introduced to break the layering inversion that had {@code HttpSupport}
 * (transport, low-level) importing {@code KsefClient} (the high-level facade).
 * {@code KsefClient} implements this interface; {@code HttpSupport} depends
 * only on the abstraction.
 */
public interface HttpRuntime {

    /** Base URL for the configured KSeF environment (no trailing slash). */
    String baseUrl();

    /** Underlying JDK HTTP client. */
    HttpClient httpClient();

    /** Session context — current JWT, reference number, etc. */
    SessionContext sessionContext();

    /** Retry executor for HTTP calls. */
    RetryHandler retryHandler();

    /** Jackson mapper configured for KSeF response/request shapes. */
    ObjectMapper objectMapper();

    /** Per-request read timeout. */
    Duration readTimeout();

    /** Trigger lazy re-authentication on HTTP 401. */
    void reauthenticate();
}
