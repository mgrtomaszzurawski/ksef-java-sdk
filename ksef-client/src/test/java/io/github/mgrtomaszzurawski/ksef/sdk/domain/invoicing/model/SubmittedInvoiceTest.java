/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link SubmittedInvoice} compact-ctor null guards + defensive
 * byte[] copies on the QR-PNG components. Plus accessor-side defensive
 * copy ({@link SubmittedInvoice#kodIQr()} / {@link SubmittedInvoice#kodIIQr()}
 * return fresh arrays on every call).
 */
class SubmittedInvoiceTest {

    private static final byte[] INVOICE_XML = "<Faktura/>".getBytes(StandardCharsets.UTF_8);
    private static final String REFERENCE = "20260418-IN-1111111111-ABCDEF1234-02";
    private static final KsefNumber KSEF_NUMBER = KsefNumber.parse("5265877635-20250826-0100001AF629-AF");
    private static final byte[] KOD_I_PNG = "kodI-png-fake-bytes".getBytes(StandardCharsets.UTF_8);
    private static final byte[] KOD_II_PNG = "kodII-png-fake-bytes".getBytes(StandardCharsets.UTF_8);
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 4, 18, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void compactCtor_rejectsNullInvoice() {
        final SessionInvoiceStatus status = newStatus();
        final Optional<KsefNumber> empty = Optional.empty();
        final Optional<byte[]> emptyBytes = Optional.empty();
        final List<String> emptyErrors = List.of();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(null, REFERENCE, status, empty, emptyBytes, emptyBytes, emptyErrors));
    }

    @Test
    void compactCtor_rejectsNullReferenceNumber() {
        final Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        final SessionInvoiceStatus status = newStatus();
        final Optional<KsefNumber> empty = Optional.empty();
        final Optional<byte[]> emptyBytes = Optional.empty();
        final List<String> emptyErrors = List.of();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(invoice, null, status, empty, emptyBytes, emptyBytes, emptyErrors));
    }

    @Test
    void compactCtor_rejectsNullStatus() {
        final Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        final Optional<KsefNumber> empty = Optional.empty();
        final Optional<byte[]> emptyBytes = Optional.empty();
        final List<String> emptyErrors = List.of();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(invoice, REFERENCE, null, empty, emptyBytes, emptyBytes, emptyErrors));
    }

    @Test
    void compactCtor_rejectsNullKsefNumberOptional() {
        final Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        final SessionInvoiceStatus status = newStatus();
        final Optional<byte[]> emptyBytes = Optional.empty();
        final List<String> emptyErrors = List.of();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(invoice, REFERENCE, status, null, emptyBytes, emptyBytes, emptyErrors));
    }

    @Test
    void compactCtor_rejectsNullKodIOptional() {
        final Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        final SessionInvoiceStatus status = newStatus();
        final Optional<KsefNumber> empty = Optional.empty();
        final Optional<byte[]> emptyBytes = Optional.empty();
        final List<String> emptyErrors = List.of();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(invoice, REFERENCE, status, empty, null, emptyBytes, emptyErrors));
    }

    @Test
    void compactCtor_rejectsNullErrorDetailsList() {
        final Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        final SessionInvoiceStatus status = newStatus();
        final Optional<KsefNumber> empty = Optional.empty();
        final Optional<byte[]> emptyBytes = Optional.empty();
        assertThrows(NullPointerException.class,
                () -> new SubmittedInvoice<>(invoice, REFERENCE, status, empty, emptyBytes, emptyBytes, null));
    }

    @Test
    void compactCtor_defensiveCopiesKodIBytes() {
        Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        SessionInvoiceStatus status = newStatus();
        byte[] sourceKodI = KOD_I_PNG.clone();
        SubmittedInvoice<Invoice> submitted = new SubmittedInvoice<>(invoice, REFERENCE, status,
                Optional.of(KSEF_NUMBER),
                Optional.of(sourceKodI),
                Optional.empty(), List.of());
        sourceKodI[0] = (byte) 0xFF;
        // Source mutation must not bleed into snapshot.
        assertTrue(submitted.kodIQr().isPresent());
        assertArrayEquals(KOD_I_PNG, submitted.kodIQr().orElseThrow());
    }

    @Test
    void kodIQr_returnsFreshCopyEachCall() {
        Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        SessionInvoiceStatus status = newStatus();
        SubmittedInvoice<Invoice> submitted = new SubmittedInvoice<>(invoice, REFERENCE, status,
                Optional.of(KSEF_NUMBER),
                Optional.of(KOD_I_PNG),
                Optional.empty(), List.of());
        byte[] first = submitted.kodIQr().orElseThrow();
        byte[] second = submitted.kodIQr().orElseThrow();
        assertNotSame(first, second);
        first[0] = (byte) 0xFF;
        // Mutating the returned copy must not affect subsequent calls.
        assertArrayEquals(KOD_I_PNG, submitted.kodIQr().orElseThrow());
    }

    @Test
    void kodIIQr_returnsFreshCopyEachCallWhenPresent() {
        Invoice invoice = Invoice.fromXml(FormCode.FA3, INVOICE_XML);
        SessionInvoiceStatus status = newStatus();
        SubmittedInvoice<Invoice> submitted = new SubmittedInvoice<>(invoice, REFERENCE, status,
                Optional.of(KSEF_NUMBER),
                Optional.empty(),
                Optional.of(KOD_II_PNG), List.of());
        byte[] first = submitted.kodIIQr().orElseThrow();
        byte[] second = submitted.kodIIQr().orElseThrow();
        assertNotSame(first, second);
        assertArrayEquals(KOD_II_PNG, first);
    }

    private static SessionInvoiceStatus newStatus() {
        return new SessionInvoiceStatus(
                1, "FV/1", KSEF_NUMBER, REFERENCE,
                null, null, NOW, NOW, null, null, null, null,
                new InvoiceStatusInfo(200, "Ok", List.of(), java.util.Map.of()));
    }
}
