/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSink;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncResult;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * and exporting invoices.
 *
 * @since 1.0.0
 */
public interface InvoiceClient {

    /**
     * Retrieve invoice XML by KSeF number. Validates length, format, and
     * CRC-8 checksum (REQ-SESS-18/19/20) before the network call.
     *
     * <p>If the consumer holds a raw string, parse it explicitly via
     * {@link KsefNumber#parse(String)} — keeping validation up-front at the
     * value-object boundary (rather than hidden behind a String overload)
     * surfaces malformed input at the first opportunity.
     */
    byte[] getByKsefNumber(KsefNumber ksefNumber);

    InvoiceMetadataResult queryInvoicesByMetadata(InvoiceQueryFilters query);

    /**
     * Stream every invoice metadata record matching the filter, walking
     * the date-cursor + page-offset model used by KSeF's
     * {@code POST /invoices/query/metadata}. Pages are fetched lazily;
     * caller controls memory pressure by limiting / collecting
     * downstream.
     */
    java.util.stream.Stream<InvoiceMetadata> streamInvoicesByMetadata(InvoiceQueryFilters query);

    InvoiceExportStatus getExportStatus(String referenceNumber);

    /**
     * Start an invoice export and return a {@link PreparedInvoiceExport} handle
     * that retains the AES key + IV needed to decrypt the returned package.
     *
     * <p>The SDK fetches the KSeF symmetric-key public key, generates the AES
     * key + IV, encrypts the AES key with the KSeF public key, sends the export
     * request, and retains the plaintext AES/IV inside the returned handle so
     * the resulting package can be downloaded and decrypted via
     * {@link PreparedInvoiceExport#downloadAndDecrypt(InvoiceExportStatus)}.
     *
     * @param query filters identifying invoices to export
     * @param fullContent {@code true} for full-content export; {@code false} for
     *     metadata-only
     * @return prepared-export handle
     */
    PreparedInvoiceExport prepareExport(InvoiceQueryFilters query, boolean fullContent);

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
    OnlineSession openSession(FormCode formCode);

    /**
     * Run an incremental sync over the consumer's invoice store.
     * Implements the documented HWM-based pagination algorithm from
     * {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}.
     *
     * <p>Tier 1 workflow API per ADR-021.
     *
     * @param plan sync configuration
     * @param checkpointStore where checkpoints are persisted between runs
     * @param sink invoice processor — called once per accepted invoice
     * @return per-subject-type counts and final checkpoints
     */
    SyncResult sync(IncrementalSyncPlan plan, CheckpointStore checkpointStore, InvoiceSink sink);

    /**
     * Stream sessions (online + batch) matching the filter, walking the
     * {@code x-continuation-token} cursor returned by KSeF
     * {@code GET /sessions} lazily. Caller controls memory pressure by
     * limiting / collecting downstream.
     *
     * @param filter required filter (type, status, date ranges, exact ref)
     * @return lazy stream of matching session summary items
     */
    java.util.stream.Stream<SessionListItem> streamSessions(SessionsQueryFilter filter);
}
