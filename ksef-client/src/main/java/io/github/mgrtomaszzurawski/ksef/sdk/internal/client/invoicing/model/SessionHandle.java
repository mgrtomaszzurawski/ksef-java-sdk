/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import org.jspecify.annotations.Nullable;

/**
 * The wire-side state of an open KSeF online session: server reference
 * number plus the symmetric encryption material agreed at session open,
 * plus the session's declared {@link FormCode} so per-invoice preflight
 * (R1-19) can verify each {@code Invoice.formCode()} matches the
 * session before any wire traffic.
 *
 * <p>{@code formCode} is nullable on purpose — legacy test fixtures
 * still build sessions without the form-code context (they use
 * {@code send(byte[])} directly instead of {@code sendInvoice(Invoice)}).
 * Production paths in {@code InvoiceSessionsImpl.online(FormCode)} always
 * supply it.
 *
 * <p>Internal handoff record — instances flow once from session-open into
 * {@code OnlineSessionImpl}'s constructor and are not compared or hashed
 * anywhere. Default record equality (reference equality on {@code byte[]}
 * components) is therefore harmless here; suppressing S6218 keeps the
 * carrier free of boilerplate equals/hashCode/toString that would never run.
 *
 * @since 1.0.0
 */
@SuppressWarnings("java:S6218")
public record SessionHandle(
        SessionClient client,
        String referenceNumber,
        byte[] aesKey,
        byte[] initVector,
        @Nullable FormCode formCode) {
}
