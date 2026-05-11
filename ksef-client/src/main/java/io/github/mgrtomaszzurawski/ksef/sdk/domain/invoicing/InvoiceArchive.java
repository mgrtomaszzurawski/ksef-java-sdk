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
    ClearedInvoice clearedFromArchive(String sessionReferenceNumber, String invoiceReferenceNumber);

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
     */
    Stream<InvoiceMetadata> streamByMetadata(InvoiceQueryRequest query);
}
