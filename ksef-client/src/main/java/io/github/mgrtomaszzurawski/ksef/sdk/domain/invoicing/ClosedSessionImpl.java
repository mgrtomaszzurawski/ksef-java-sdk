/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private final SessionClient sessionClient;
    private final String referenceNumber;

    ClosedSessionImpl(SessionClient sessionClient, String referenceNumber) {
        this.sessionClient = sessionClient;
        this.referenceNumber = referenceNumber;
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
    public byte[] upo(String invoiceReferenceNumber) {
        return sessionClient.getUpoByInvoiceReference(referenceNumber, invoiceReferenceNumber);
    }

    @Override
    public byte[] upoByKsefNumber(KsefNumber ksefNumber) {
        return sessionClient.getUpoByKsefNumber(referenceNumber, ksefNumber);
    }

    @Override
    public List<byte[]> bulkUpos() {
        SessionStatus current = sessionClient.getStatus(referenceNumber);
        if (current.upo() == null || current.upo().pages() == null || current.upo().pages().isEmpty()) {
            return List.of();
        }
        List<byte[]> pages = new ArrayList<>(current.upo().pages().size());
        for (var page : current.upo().pages()) {
            pages.add(sessionClient.getUpoByReference(referenceNumber, page.referenceNumber()));
        }
        return List.copyOf(pages);
    }

    @Override
    public void close() {
        // Already closed — pure no-op per Session.close() / AutoCloseable contract.
    }
}
