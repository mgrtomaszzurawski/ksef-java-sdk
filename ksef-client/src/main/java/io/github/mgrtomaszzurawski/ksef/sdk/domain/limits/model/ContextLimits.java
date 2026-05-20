/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

import org.jspecify.annotations.Nullable;

/**
 * Snapshot of the maximum-per-session caps the server enforces on
 * online and batch sessions for the current authentication context.
 * Both fields are populated by the server (both required); the
 * {@link Nullable} annotation guards against malformed responses.
 *
 * @param onlineSession per-session caps for interactive (online)
 *     sessions opened via {@code client.invoices().sessions().online(...)}.
 *     Contains three caps: single-invoice size in MB,
 *     invoice-with-attachment size in MB, and total invoices per
 *     session.
 * @param batchSession per-session caps for batch package submissions
 *     driven via {@code client.invoices().sessions().batch().submit(...)}.
 *     Same three caps apply per individual invoice part inside the
 *     batch.
 *
 * @since 0.1.0
 */
public record ContextLimits(@Nullable OnlineSessionLimits onlineSession, @Nullable BatchSessionLimits batchSession) {

}
