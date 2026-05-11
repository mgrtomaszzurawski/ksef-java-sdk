/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import java.nio.file.Path;
import java.util.List;

/**
 * Client for KSeF invoice operations — querying metadata, retrieving by KSeF number,
 * exporting invoices, opening online sessions, and submitting batches.
 *
 * <p><strong>Threading warning for batch methods:</strong> the
 * {@link #submitBatch(FormCode, List, BatchOptions)} and
 * {@link #submitBatchFromFiles(FormCode, List, BatchOptions)} methods block the
 * calling thread for minutes to hours, depending on batch size and upload
 * bandwidth. KSeF batch can be up to 5 GB. Do not call from UI threads, HTTP
 * request handlers, or reactive framework dispatch threads. Wrap with a
 * dedicated executor for async use.
 *
 * @since 1.0.0
 */
public interface InvoiceClient {

    /**
     * Retrieve a typed {@link InvoiceDocument} by KSeF number. Validates
     * length, format, and CRC-8 checksum (REQ-SESS-18/19/20) before the
     * network call. Branches on the FormCode detected from the response
     * XML root element + namespace and returns the matching typed
     * subclass ({@code Fa3InvoiceDocument}, {@code Fa2InvoiceDocument},
     * {@code PefInvoiceDocument}, {@code PefKorInvoiceDocument}). Unknown
     * / custom schemas fall through to the minimal
     * {@link InvoiceDocument#fromXml(FormCode, byte[])} wrapper.
     *
     * <p>If the consumer holds a raw string, parse it explicitly via
     * {@link KsefNumber#parse(String)} — keeping validation up-front at the
     * value-object boundary (rather than hidden behind a String overload)
     * surfaces malformed input at the first opportunity.
     */
    InvoiceDocument getByKsefNumber(KsefNumber ksefNumber);

    /**
     * Reconstruct a fully recovered {@link ClearedInvoice} from KSeF given
     * only the {@code (sessionReferenceNumber, invoiceReferenceNumber)} pair
     * the consumer persisted across a process restart.
     *
     * <p>Composes three existing server calls:
     * <ol>
     *   <li>{@code GET /sessions/{sessionRef}/invoices/{invoiceRef}} —
     *       retrieves the session-side {@code SessionInvoiceStatus} (status
     *       code, ordinal, KSeF number when accepted).</li>
     *   <li>{@code GET /invoices/ksef/{ksefNumber}} — fetches the archived
     *       invoice XML and detects the FormCode from its root element.</li>
     *   <li>{@code GET /sessions/{sessionRef}/invoices/{invoiceRef}/upo} —
     *       downloads the UPO XAdES bytes.</li>
     * </ol>
     *
     * <p>The returned {@link ClearedInvoice} carries a fresh
     * {@code SubmittedInvoice} reconstructed from the recovered state; KOD I
     * and KOD II QR PNGs are left empty in this view (consumers that need
     * them can regenerate via
     * {@code KsefClient.qrCode().generateKodIQr(...)} using the recovered
     * invoice metadata).
     *
     * <p>Spec endpoints used: {@code /sessions/{ref}/invoices/{invRef}},
     * {@code /sessions/{ref}/invoices/{invRef}/upo}, {@code /invoices/ksef/{n}}.
     *
     * @throws io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException
     *     when the invoice has not reached terminal status 200 (success);
     *     the consumer should not be calling this method for non-accepted
     *     invoices since no UPO exists yet.
     */
    ClearedInvoice clearedFromArchive(String sessionReferenceNumber, String invoiceReferenceNumber);

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
     * Stream-based incremental sync — returns a lazy {@link java.util.stream.Stream}
     * of {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice}
     * elements walked across the configured subject types and date windows.
     *
     * <p>Stream is {@link AutoCloseable} — caller MUST consume via
     * try-with-resources to release the underlying paginator and ensure
     * the final checkpoint commit. Each consumed-and-not-skipped element
     * advances the {@code checkpointStore} atomically per element so a
     * caller breaking out early via {@link java.util.stream.Stream#limit(long)}
     * leaves the checkpoint at the last successfully consumed element.
     *
     * <p>Spec citation: {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}.
     *
     * @param plan sync configuration
     * @param checkpointStore where checkpoints are persisted between runs
     * @return lazy {@link java.util.stream.Stream} of decrypted invoices
     */
    java.util.stream.Stream<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice>
            syncAsStream(IncrementalSyncPlan plan, CheckpointStore checkpointStore);

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

    /**
     * Submit a batch of invoices synchronously. SDK encrypts every invoice with
     * a fresh AES session key, splits the encrypted ZIP into parts, opens a
     * KSeF batch session, uploads every part, closes the session, polls until
     * KSeF reports a terminal state, and downloads UPOs for accepted invoices.
     *
     * <p>By the time this method returns, every accepted invoice's UPO is
     * already in {@link BatchResult#cleared()}.
     *
     * <p><strong>Warning:</strong> This method blocks the calling thread for
     * minutes to hours, depending on batch size and upload bandwidth. KSeF batch
     * can be up to 5 GB. Do not call from UI threads, HTTP request handlers, or
     * reactive framework dispatch threads. Wrap with a dedicated executor for
     * async use.
     *
     * @param formCode form code for the batch — must match every invoice's own
     *     {@link Invoice#formCode()}
     * @param invoices invoices to submit (non-empty)
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     */
    BatchResult submitBatch(FormCode formCode, List<Invoice> invoices, BatchOptions options);

    /**
     * File-streaming variant of {@link #submitBatch(FormCode, List, BatchOptions)}.
     * Each invoice is read straight from disk into the batch ZIP rather than
     * materialised as a {@code byte[]} in heap. Use this for large batches —
     * e.g. the spec cap of 10 000 invoices (REQ-SESS-41) — so peak heap stays
     * bounded by the chunk-encryption buffer.
     *
     * <p><strong>Warning:</strong> This method blocks the calling thread for
     * minutes to hours, depending on batch size and upload bandwidth. KSeF batch
     * can be up to 5 GB. Do not call from UI threads, HTTP request handlers, or
     * reactive framework dispatch threads. Wrap with a dedicated executor for
     * async use.
     *
     * @param formCode form code for the batch
     * @param files non-empty list of paths to invoice XML files
     * @param options runtime tunables (timeout, parallelism)
     * @return {@link BatchResult} populated with cleared + failed entries
     */
    BatchResult submitBatchFromFiles(FormCode formCode, List<Path> files, BatchOptions options);
}
