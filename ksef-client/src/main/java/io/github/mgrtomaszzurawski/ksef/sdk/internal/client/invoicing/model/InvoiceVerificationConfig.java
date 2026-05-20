/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for the synchronous invoice-verification flow inside an
 * online session: server-supplied {@code validUntil} cap, target
 * environment (used to render KOD I links), and the polling deadline.
 *
 * <p>Each field is nullable on purpose — legacy fixtures still build
 * sessions without the verification context (and therefore use
 * {@code send(byte[])} directly instead of {@code sendInvoice(Invoice)}).
 *
 * @since 0.1.0
 */
public record InvoiceVerificationConfig(
        @Nullable OffsetDateTime validUntil,
        @Nullable KsefEnvironment environment,
        Duration invoiceVerificationTimeout) {
}
