/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import java.util.stream.Stream;

/**
 * Archive-side invoice access — retrieve by KSeF number, reconstruct a
 * {@link ClearedInvoice} from persisted reference pair, and query/stream
 * invoice metadata.
 *
 * <p>Reached via {@link Invoices#archive()}.
 *
 * <p><strong>Typical flows</strong>:
 *
 * <ol>
 *   <li><b>Direct lookup by ksefNumber</b> — when you have only the
 *       KSeF-assigned invoice number (e.g. from
 *       {@link #queryByMetadata(InvoiceQueryRequest)} or an external
 *       system), use {@link #getByKsefNumber(KsefNumber)}. Returns the
 *       typed {@link InvoiceDocument} (XML view); UPO is <em>not</em>
 *       included because KSeF exposes UPO only via session-bound
 *       endpoints. To obtain UPO you need the session/invoice
 *       reference pair persisted at submission time.</li>
 *
 *   <li><b>Recovery from persisted state</b> — when you persisted the
 *       {@code (sessionReferenceNumber, invoiceReferenceNumber)} pair
 *       at submission time and want to recover the full
 *       {@link ClearedInvoice} (document + UPO), use
 *       {@link #clearedFromArchive(String, String)}. Composes three
 *       server calls under the hood. Throws if the invoice has not
 *       reached terminal accepted status (no UPO exists yet).</li>
 *
 *   <li><b>Bulk discovery</b> — to walk many invoices by filter (date
 *       range, party, amount, etc.), use
 *       {@link #queryByMetadata(InvoiceQueryRequest)} (single page
 *       with {@code hasMore}/{@code totalCount}) or
 *       {@link #streamByMetadata(InvoiceQueryRequest)} (lazy paging
 *       across full result set). Both return {@link InvoiceMetadata}
 *       summaries <em>without</em> XML — server-side optimisation
 *       against bandwidth blowup. To fetch full documents for query
 *       results, iterate and call {@link #getByKsefNumber} per item:
 *       <pre>{@code
 *       List<InvoiceDocument> docs = client.invoices().archive()
 *               .streamByMetadata(query)
 *               .map(meta -> client.invoices().archive()
 *                       .getByKsefNumber(meta.ksefNumber()))
 *               .toList();
 *       }</pre>
 *       For mass-archival download, prefer the async export flow
 *       (KSeF spec: {@code POST /invoices/async/exports}) which bundles
 *       up to 10000 invoices / 1 GB into a single encrypted ZIP.</li>
 * </ol>
 *
 * <p><strong>Why no {@code getClearedByKsefNumber}?</strong> KSeF does
 * not expose UPO retrieval by ksefNumber — every UPO endpoint requires
 * the original session reference. From a ksefNumber alone, UPO is
 * server-side unreachable, so the SDK does not provide a fake
 * composite that would always return a half-empty result. UPO is a
 * compliance artefact of the submitting session; persist the UPO
 * bytes (and the session/invoice ref pair) at submission time.
 *
 * @since 1.0.0
 */
public interface InvoiceArchive {

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
    ClearedInvoice<Invoice> clearedFromArchive(String sessionReferenceNumber, String invoiceReferenceNumber);

    /**
     * Query invoice metadata with filters (date range, buyer/seller, amounts, etc.).
     *
     * @param query the filter criteria
     * @return paginated list of invoice metadata
     */
    InvoiceMetadataResult queryByMetadata(InvoiceQueryRequest query);

    /**
     * Stream every invoice metadata record matching the filter, walking
     * the date-cursor + page-offset model used by KSeF's
     * {@code POST /invoices/query/metadata}. Pages are fetched lazily;
     * caller controls memory pressure by limiting / collecting
     * downstream.
     *
     * <p>{@code query.pageOffset()} is <em>ignored</em> — the paginator
     * always starts from page 0 and iterates until the server reports
     * {@code hasMore == false}. Use {@link #queryByMetadata} for a
     * snapshot at a specific offset.
     */
    Stream<InvoiceMetadata> streamByMetadata(InvoiceQueryRequest query);
}
