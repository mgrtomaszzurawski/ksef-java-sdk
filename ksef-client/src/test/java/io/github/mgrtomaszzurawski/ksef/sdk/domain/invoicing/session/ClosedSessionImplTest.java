/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;

import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceStatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-coverage for the package-private {@link ClosedSessionImpl} facade.
 * Mocks {@link SessionClient} so each public-API method is exercised
 * independently of the wire layer. ClosedSessionImpl is constructed
 * only by {@link OnlineSessionImpl#archive()} in production, so direct
 * instantiation here uses the same package-private constructor.
 */
class ClosedSessionImplTest {

    private static final String SESSION_REF = "20260418-SE-1111111111-ABCDEF1234-01";
    private static final String INVOICE_REF = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final String KSEF_NUMBER_VALUE = "5265877635-20250826-0100001AF629-AF";
    private static final OffsetDateTime CREATED = OffsetDateTime.of(2026, 4, 18, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime VALID_UNTIL = CREATED.plusHours(2);
    private static final byte[] UPO_BYTES = "<UPO/>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVOICE_XML = "<Faktura/>".getBytes(StandardCharsets.UTF_8);

    @Test
    void referenceNumber_returnsCtorArgument() {
        SessionClient sessionClient = mock(SessionClient.class);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        assertEquals(SESSION_REF, closed.referenceNumber());
    }

    @Test
    void status_proxiesToSessionClient() {
        SessionClient sessionClient = mock(SessionClient.class);
        SessionStatus stub = newStatus();
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(stub);

        SessionStatus result = new ClosedSessionImpl(sessionClient, SESSION_REF).status();

        assertSame(stub, result);
        verify(sessionClient).getStatus(SESSION_REF);
    }

    @Test
    void dateCreated_returnsValueFromStatusSnapshot() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(newStatus());
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        assertEquals(CREATED, closed.dateCreated());
    }

    @Test
    void validUntil_returnsOptionalFromStatusSnapshot() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(newStatus());
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        Optional<OffsetDateTime> result = closed.validUntil();
        assertTrue(result.isPresent());
        assertEquals(VALID_UNTIL, result.get());
    }

    @Test
    void invoiceCount_returnsEmptyWhenStatusInvoiceCountNull() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(newStatus());
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        assertEquals(Optional.empty(), closed.totalInvoiceCount());
        assertEquals(Optional.empty(), closed.successfulInvoiceCount());
        assertEquals(Optional.empty(), closed.failedInvoiceCount());
    }

    @Test
    void invoiceCounters_returnPresentWhenStatusCarriesValues() {
        SessionClient sessionClient = mock(SessionClient.class);
        SessionStatus stub = new SessionStatus(
                new StatusInfo(200, "Ok", List.of()),
                CREATED, CREATED, VALID_UNTIL, null, 10, 7, 3);
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(stub);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        assertEquals(Optional.of(10), closed.totalInvoiceCount());
        assertEquals(Optional.of(7), closed.successfulInvoiceCount());
        assertEquals(Optional.of(3), closed.failedInvoiceCount());
    }

    @Test
    void invoices_wrapsSessionClientResponseInSessionInvoices() {
        SessionClient sessionClient = mock(SessionClient.class);
        SessionInvoiceStatus entry = newAcceptedInvoiceStatus(1);
        when(sessionClient.getAllInvoices(SESSION_REF)).thenReturn(List.of(entry));
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        SessionInvoices result = closed.invoices();
        assertEquals(1, result.invoices().size());
        verify(sessionClient).getAllInvoices(SESSION_REF);
    }

    @Test
    void invoiceStatus_proxiesToSessionClient() {
        SessionClient sessionClient = mock(SessionClient.class);
        SessionInvoiceStatus stub = newAcceptedInvoiceStatus(1);
        when(sessionClient.getInvoiceStatus(SESSION_REF, INVOICE_REF)).thenReturn(stub);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        SessionInvoiceStatus result = closed.invoiceStatus(INVOICE_REF);
        assertSame(stub, result);
    }

    @Test
    void failedInvoices_proxiesToSessionClient() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getAllFailedInvoices(SESSION_REF)).thenReturn(List.of());
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        SessionInvoices result = closed.failedInvoices();
        assertEquals(0, result.invoices().size());
    }

    @Test
    void cleared_bySubmittedInvoice_buildsTypedClearedInvoice() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getUpoByInvoiceReference(SESSION_REF, INVOICE_REF)).thenReturn(UPO_BYTES);

        Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        SessionInvoiceStatus status = newAcceptedInvoiceStatus(1);
        SubmittedInvoice<Invoice> submitted = new SubmittedInvoice<>(invoice, INVOICE_REF, status,
                Optional.of(KsefNumber.parse(KSEF_NUMBER_VALUE)),
                Optional.empty(), Optional.empty(), List.of());

        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        ClearedInvoice<Invoice> result = closed.cleared(submitted);

        assertSame(submitted, result.submitted());
        assertEquals(INVOICE_REF, result.upo().referenceNumber());
        verify(sessionClient).getUpoByInvoiceReference(SESSION_REF, INVOICE_REF);
    }

    @Test
    void cleared_bySubmittedInvoice_nullArgThrows() {
        SessionClient sessionClient = mock(SessionClient.class);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        assertThrows(NullPointerException.class, () -> closed.cleared((SubmittedInvoice<?>) null));
    }

    @Test
    void cleared_byInvoiceReference_buildsSyntheticSubmittedFromUpoOnly() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getUpoByInvoiceReference(SESSION_REF, INVOICE_REF)).thenReturn(UPO_BYTES);
        SessionInvoiceStatus status = newAcceptedInvoiceStatus(1);
        when(sessionClient.getInvoiceStatus(SESSION_REF, INVOICE_REF)).thenReturn(status);

        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        ClearedInvoice<Invoice> result = closed.cleared(INVOICE_REF);

        assertEquals(INVOICE_REF, result.upo().referenceNumber());
        assertEquals(INVOICE_REF, result.submitted().referenceNumber());
        assertTrue(result.submitted().ksefNumber().isPresent());
        assertEquals(KsefNumber.parse(KSEF_NUMBER_VALUE), result.submitted().ksefNumber().orElseThrow());
    }

    @Test
    void cleared_byInvoiceReference_nullArgThrows() {
        SessionClient sessionClient = mock(SessionClient.class);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        assertThrows(NullPointerException.class, () -> closed.cleared((String) null));
    }

    @Test
    void allCleared_whenUpoMissing_returnsEmptyList() {
        SessionClient sessionClient = mock(SessionClient.class);
        when(sessionClient.getStatus(SESSION_REF)).thenReturn(newStatus());
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);

        assertTrue(closed.allCleared().isEmpty());
    }

    @Test
    void close_isNoOpAfterAlreadyClosedState() {
        SessionClient sessionClient = mock(SessionClient.class);
        ClosedSessionImpl closed = new ClosedSessionImpl(sessionClient, SESSION_REF);
        // Pure no-op per AutoCloseable contract — double-close must not throw
        // and must not delegate to the SessionClient.
        closed.close();
        closed.close();
        org.mockito.Mockito.verifyNoInteractions(sessionClient);
    }

    private static SessionStatus newStatus() {
        return new SessionStatus(
                new StatusInfo(200, "Ok", List.of()),
                CREATED, CREATED, VALID_UNTIL, null, null, null, null);
    }

    private static SessionInvoiceStatus newAcceptedInvoiceStatus(int ordinal) {
        return new SessionInvoiceStatus(
                ordinal, "FV/" + ordinal, KsefNumber.parse(KSEF_NUMBER_VALUE), INVOICE_REF,
                null, null, CREATED, CREATED, null, null, null, null,
                new InvoiceStatusInfo(200, "Ok", List.of(), java.util.Map.of()));
    }
}
