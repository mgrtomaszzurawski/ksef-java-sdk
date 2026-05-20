/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceStatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoSummary;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.InvoiceDocumentConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Package-private implementation of {@link ClosedSession}. Backed by
 * the same {@link SessionClient} as the originating
 * {@link OnlineSessionImpl} — the session reference is fixed at
 * construction time and the underlying KSeF state is already in the
 * closed/terminal range, so all read methods proxy directly to the
 * server.
 *
 * <p>Constructed only by {@link OnlineSessionImpl#archive()} (and by
 * {@code OnlineSessionImpl#close()}, which records the canonical
 * closed-view for any later {@code archive()} caller). Never
 * instantiated by consumer code.
 *
 * @since 1.0.0
 */
final class ClosedSessionImpl implements ClosedSession {

    private static final String ERR_NULL_SUBMITTED = "submitted must not be null";
    private static final String ERR_NULL_INVOICE_REF = "invoiceReferenceNumber must not be null";

    /** Sentinel form-code marking the synthetic placeholder used when rebuilding
     *  {@link SubmittedInvoice} from a UPO-only context (no original invoice XML). */
    private static final FormCode UPO_PLACEHOLDER_FORM_CODE = FormCode.custom("UPO", "1", "UPO");
    /** Empty payload for the UPO-only placeholder — the real invoice is unavailable
     *  through the UPO archive path; consumers needing the original XML must call
     *  {@code client.invoices().archive().getByKsefNumber(...)}. */
    private static final byte[] UPO_PLACEHOLDER_XML = new byte[0];
    /** Synthetic terminal-status code recorded on rebuilt {@link SessionInvoiceStatus}
     *  entries — UPO only contains accepted invoices, so the status is always 200-Ok. */
    private static final int UPO_ACCEPTED_STATUS_CODE = 200;
    private static final String UPO_ACCEPTED_STATUS_DESCRIPTION = "Accepted";

    private final SessionClient sessionClient;
    private final String referenceNumber;
    private final KsefInvoiceTypes invoiceTypes;

    ClosedSessionImpl(SessionClient sessionClient, String referenceNumber) {
        this(sessionClient, referenceNumber, KsefInvoiceTypes.builtinsOnly());
    }

    ClosedSessionImpl(SessionClient sessionClient, String referenceNumber, KsefInvoiceTypes invoiceTypes) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
        this.invoiceTypes = Objects.requireNonNull(invoiceTypes, "invoiceTypes must not be null");
    }

    @Override
    public String referenceNumber() {
        return referenceNumber;
    }

    @Override
    public SessionStatus status() {
        return sessionClient.getStatus(referenceNumber);
    }

    @Override
    public OffsetDateTime dateCreated() {
        return status().dateCreated();
    }

    @Override
    public Optional<OffsetDateTime> validUntil() {
        return Optional.ofNullable(status().validUntil());
    }

    @Override
    public Optional<Integer> totalInvoiceCount() {
        return Optional.ofNullable(status().invoiceCount());
    }

    @Override
    public Optional<Integer> successfulInvoiceCount() {
        return Optional.ofNullable(status().successfulInvoiceCount());
    }

    @Override
    public Optional<Integer> failedInvoiceCount() {
        return Optional.ofNullable(status().failedInvoiceCount());
    }

    @Override
    public SessionInvoices invoices() {
        return new SessionInvoices(null, sessionClient.getAllInvoices(referenceNumber));
    }

    @Override
    public SessionInvoiceStatus invoiceStatus(String invoiceRef) {
        return sessionClient.getInvoiceStatus(referenceNumber, invoiceRef);
    }

    @Override
    public SessionInvoices failedInvoices() {
        return new SessionInvoices(null, sessionClient.getAllFailedInvoices(referenceNumber));
    }

    @Override
    public <I extends Invoice> ClearedInvoice<I> cleared(SubmittedInvoice<I> submitted) {
        Objects.requireNonNull(submitted, ERR_NULL_SUBMITTED);
        byte[] xml = sessionClient.getUpoByInvoiceReference(referenceNumber, submitted.referenceNumber());
        UpoEntry entry = new UpoEntry(submitted.referenceNumber(), xml);
        // R1-6: dispatch on the embedded Invoice's formCode to produce a
        // typed InvoiceDocument when the schema is known (FA2/FA3/PEF/PEF_KOR).
        // Custom forms fall back to the anonymous wrapper. ClosedSession
        // archive flow doesn't refetch the archived bytes — the bytes used
        // here are the original submission XML the consumer already held.
        InvoiceDocument document = InvoiceDocumentConstructor.newDocument(
                invoiceTypes, submitted.invoice().formCode(), submitted.invoice().xml());
        return new ClearedInvoice<>(submitted, document, entry);
    }

    /**
     * Convenience overload — fetches the UPO for a single invoice reference and
     * rebuilds a minimal {@link SubmittedInvoice} from KSeF-side state.
     *
     * <p>The embedded {@link Invoice} is the {@link #UPO_PLACEHOLDER_FORM_CODE}
     * sentinel — the original FA(3)/PEF/PEFKOR invoice XML is not retained on
     * the session-archive path, so consumers needing the canonical invoice
     * payload must call {@code client.invoices().archive().getByKsefNumber(...)} with the
     * {@link SubmittedInvoice#ksefNumber()} value when present.
     */
    @Override
    public ClearedInvoice<Invoice> cleared(String invoiceReferenceNumber) {
        Objects.requireNonNull(invoiceReferenceNumber, ERR_NULL_INVOICE_REF);
        byte[] xml = sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
        SessionInvoiceStatus status = sessionClient.getInvoiceStatus(referenceNumber, invoiceReferenceNumber);
        Invoice placeholder = Invoice.fromXml(UPO_PLACEHOLDER_FORM_CODE, UPO_PLACEHOLDER_XML);
        InvoiceDocument documentPlaceholder = InvoiceDocuments.anonymousFromXml(
                UPO_PLACEHOLDER_FORM_CODE, UPO_PLACEHOLDER_XML);
        SubmittedInvoice<Invoice> submitted = new SubmittedInvoice<>(
                placeholder, invoiceReferenceNumber, status,
                status.ksefNumber() != null
                        ? Optional.of(status.ksefNumber())
                        : Optional.empty(),
                Optional.empty(), Optional.empty(), List.of());
        return new ClearedInvoice<>(submitted, documentPlaceholder,
                new UpoEntry(invoiceReferenceNumber, xml));
    }

    /**
     * Bulk-rebuild every cleared invoice from the closed session's UPO pages.
     *
     * <p>UPO XML is parsed once per page via {@link UpoSummary#parse(byte[])},
     * and each {@link KsefNumber} listed in the page yields one
     * {@link ClearedInvoice}. The per-ksef-number {@code getInvoiceStatus(...)}
     * round-trip from the previous implementation is dropped — UPO only lists
     * accepted invoices, so a synthetic 200-Ok {@link SessionInvoiceStatus}
     * carries the canonical terminal state.
     *
     * <p>The embedded {@link Invoice} is the {@link #UPO_PLACEHOLDER_FORM_CODE}
     * sentinel; see {@link #cleared(String)} for the rationale and the
     * {@code getByKsefNumber(...)} round-trip needed when consumers need the
     * original XML.
     */
    @Override
    public List<ClearedInvoice<Invoice>> allCleared() {
        SessionStatus current = sessionClient.getStatus(referenceNumber);
        if (current.upo() == null || current.upo().pages() == null || current.upo().pages().isEmpty()) {
            return List.of();
        }
        List<ClearedInvoice<Invoice>> bulk = new ArrayList<>(current.upo().pages().size());
        int ordinalCounter = 0;
        for (var page : current.upo().pages()) {
            byte[] xml = sessionClient.getUpoByReference(referenceNumber, page.referenceNumber());
            UpoSummary summary = UpoSummary.parse(xml);
            for (KsefNumber ksefNumber : summary.ksefNumbers()) {
                String invoiceRef = ksefNumber.value();
                ordinalCounter++;
                SessionInvoiceStatus syntheticStatus = newAcceptedSessionInvoiceStatus(
                        ordinalCounter, invoiceRef, ksefNumber, summary.acceptanceDate());
                Invoice placeholder = Invoice.fromXml(UPO_PLACEHOLDER_FORM_CODE, UPO_PLACEHOLDER_XML);
                InvoiceDocument documentPlaceholder = InvoiceDocuments.anonymousFromXml(
                        UPO_PLACEHOLDER_FORM_CODE, UPO_PLACEHOLDER_XML);
                SubmittedInvoice<Invoice> rebuilt = new SubmittedInvoice<>(
                        placeholder, invoiceRef, syntheticStatus,
                        Optional.of(ksefNumber), Optional.empty(), Optional.empty(), List.of());
                bulk.add(new ClearedInvoice<>(rebuilt, documentPlaceholder,
                        new UpoEntry(invoiceRef, xml)));
            }
        }
        return List.copyOf(bulk);
    }

    /**
     * Build a synthetic {@link SessionInvoiceStatus} for a UPO-listed invoice.
     * UPO is the canonical proof-of-acceptance, so the status is always
     * {@link #UPO_ACCEPTED_STATUS_CODE}-Ok. {@code invoiceNumber} is unknown
     * from the UPO context — the rebuilt status uses the KSeF reference as a
     * placeholder so the record's non-null contract is honoured.
     */
    private static SessionInvoiceStatus newAcceptedSessionInvoiceStatus(
            int ordinal, String invoiceRef, KsefNumber ksefNumber, OffsetDateTime acceptedAt) {
        InvoiceStatusInfo statusInfo = new InvoiceStatusInfo(
                UPO_ACCEPTED_STATUS_CODE, UPO_ACCEPTED_STATUS_DESCRIPTION, List.of(), Map.of());
        return new SessionInvoiceStatus(
                ordinal, invoiceRef, ksefNumber, invoiceRef,
                null, null, acceptedAt, acceptedAt, null, null, null, null, statusInfo);
    }

    @Override
    public void close() {
        // Already closed — pure no-op per Session.close() / AutoCloseable contract.
    }
}
