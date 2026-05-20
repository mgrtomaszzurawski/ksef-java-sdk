/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceBatch;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryRequest;
import java.util.stream.Stream;

/**
 * Full KSeF session lifecycle access — open interactive (online) sessions
 * for one-by-one sends, drive batch package submissions, and stream
 * session summaries (covering both online and batch).
 *
 * <p>Reached via {@link Invoices#sessions()}.
 *
 * @since 0.1.0
 */
public interface InvoiceSessions {

    /**
     * Open an interactive (online) KSeF session for sending invoices.
     *
     * <p>Authenticates lazily on the parent {@code KsefClient} if not already
     * authenticated. Generates an AES encryption key, encrypts it with the
     * KSeF public key, and opens the session. The returned
     * {@link OnlineSession} handles all invoice encryption internally.
     *
     * <p>KSeF allows only one active online session per NIP at a time.
     *
     * <p><strong>Cooldown after termination.</strong> After a terminated
     * online session, the server enforces a ~30-60 s cooldown for the same
     * NIP. A new session opened too soon will return a reference number
     * but reject the first {@code send(...)} with HTTP 415. The SDK
     * translates that into
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException}
     * with a
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException#suggestedRetryAfter()}
     * recommendation.
     *
     * @param formCode the invoice form code (e.g. {@link FormCode#FA3})
     * @return an open session — use with try-with-resources
     */
    OnlineSession online(FormCode formCode);

    /**
     * Access the batch session flow — synchronous submission of up to
     * 10 000 invoices in a single encrypted package. The returned
     * {@link InvoiceBatch} blocks for minutes to hours; see its Javadoc
     * for the threading contract.
     *
     * @return batch session accessor (non-null)
     */
    InvoiceBatch batch();

    /**
     * Stream sessions (online + batch) matching the filter, walking the
     * {@code x-continuation-token} cursor returned by KSeF
     * {@code GET /sessions} lazily. Caller controls memory pressure by
     * limiting / collecting downstream.
     *
     * @param filter required filter (type, status, date ranges, exact ref)
     * @return lazy stream of matching session summary items
     */
    Stream<SessionListItem> stream(SessionsQueryRequest filter);
}
