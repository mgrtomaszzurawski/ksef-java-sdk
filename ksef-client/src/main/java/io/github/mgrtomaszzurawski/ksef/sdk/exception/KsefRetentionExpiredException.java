/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import org.jspecify.annotations.Nullable;

/**
 * Thrown on HTTP 410 (Gone) — the requested asynchronous result was
 * removed by KSeF retention policy (api-changelog v2.4.0).
 *
 * <p>Retention windows on KSeF: auth challenges 7d, export packages 7d,
 * certificate enrollments 30d, permission operation statuses 30d.
 * Callers polling a long-running operation may observe this when the
 * server has discarded the result before the poll completed.
 *
 * <p>Extends {@link KsefNotFoundException} for source-compatible
 * handling — pre-1.0 code that catches {@link KsefNotFoundException}
 * still works. Code that needs to distinguish "expired by retention"
 * from "never existed" should catch this subtype before the parent.
 *
 * @since 0.1.0
 */
@SuppressWarnings("java:S110")
public class KsefRetentionExpiredException extends KsefNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KsefRetentionExpiredException(String message, @Nullable Throwable cause, int statusCode, @Nullable String responseBody) {
        super(message, cause, statusCode, responseBody);
    }
}
