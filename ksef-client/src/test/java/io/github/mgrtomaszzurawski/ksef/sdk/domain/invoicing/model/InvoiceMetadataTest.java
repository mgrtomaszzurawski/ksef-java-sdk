/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link InvoiceMetadata} compact-ctor defensive copies + equals
 * / hashCode / toString over byte[] components. The 20-field record
 * carries two nullable byte[] fields ({@code invoiceHash},
 * {@code hashOfCorrectedInvoice}) that must be cloned on construction
 * AND on every accessor call, plus a defensively-copied
 * {@code thirdSubjects} list.
 */
class InvoiceMetadataTest {

    private static final KsefNumber KSEF_NUMBER = KsefNumber.parse("5265877635-20250826-0100001AF629-AF");
    private static final String INVOICE_NUMBER = "FV/2026/001";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 4, 18);
    private static final OffsetDateTime INVOICING_DATE = OffsetDateTime.of(2026, 4, 18, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final byte[] HASH_BYTES = "invoice-hash-32-bytes-fake-data!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CORRECTION_HASH = "correction-hash-32-bytes-data!!!".getBytes(StandardCharsets.UTF_8);

    @Test
    void compactCtor_defensiveCopiesInvoiceHashAndCorrectionHash() {
        byte[] source = HASH_BYTES.clone();
        byte[] correction = CORRECTION_HASH.clone();
        InvoiceMetadata metadata = newMetadata(source, correction);
        source[0] = (byte) 0xFF;
        correction[0] = (byte) 0xFF;
        // Source mutation must not bleed into the record's snapshot.
        assertEquals('i', (char) metadata.invoiceHash()[0]);
        assertEquals('c', (char) metadata.hashOfCorrectedInvoice()[0]);
    }

    @Test
    void invoiceHash_returnsFreshCopyEachCall() {
        InvoiceMetadata metadata = newMetadata(HASH_BYTES, CORRECTION_HASH);
        byte[] first = metadata.invoiceHash();
        byte[] second = metadata.invoiceHash();
        assertNotSame(first, second);
        assertArrayEquals(first, second);
        first[0] = (byte) 0xFF;
        // Mutating the returned copy must not affect subsequent calls.
        assertEquals('i', (char) metadata.invoiceHash()[0]);
    }

    @Test
    void hashOfCorrectedInvoice_returnsFreshCopyEachCall() {
        InvoiceMetadata metadata = newMetadata(HASH_BYTES, CORRECTION_HASH);
        byte[] first = metadata.hashOfCorrectedInvoice();
        byte[] second = metadata.hashOfCorrectedInvoice();
        assertNotSame(first, second);
        assertArrayEquals(first, second);
    }

    @Test
    void hashes_returnNullWhenAbsentOnConstruction() {
        InvoiceMetadata metadata = newMetadata(null, null);
        assertNull(metadata.invoiceHash());
        assertNull(metadata.hashOfCorrectedInvoice());
    }

    @Test
    void thirdSubjects_areCopiedDefensivelyOnConstruction() {
        java.util.ArrayList<InvoiceThirdSubject> mutable = new java.util.ArrayList<>();
        InvoiceMetadata metadata = new InvoiceMetadata(
                KSEF_NUMBER, INVOICE_NUMBER, ISSUE_DATE, INVOICING_DATE,
                INVOICING_DATE, INVOICING_DATE, null, null, null, null,
                null, null, null, null, null, null, null, null, null, mutable);
        // Adding to the original list after construction must not affect the snapshot.
        assertEquals(0, metadata.thirdSubjects().size());
    }

    @Test
    void thirdSubjects_nullInputDefaultsToEmptyList() {
        InvoiceMetadata metadata = new InvoiceMetadata(
                KSEF_NUMBER, INVOICE_NUMBER, ISSUE_DATE, INVOICING_DATE,
                INVOICING_DATE, INVOICING_DATE, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        assertEquals(0, metadata.thirdSubjects().size());
    }

    @Test
    void equalsAndHashCode_areStructural() {
        InvoiceMetadata first = newMetadata(HASH_BYTES, CORRECTION_HASH);
        InvoiceMetadata second = newMetadata(HASH_BYTES.clone(), CORRECTION_HASH.clone());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equals_isFalseWhenInvoiceHashDiffers() {
        InvoiceMetadata baseline = newMetadata(HASH_BYTES, CORRECTION_HASH);
        byte[] mutated = HASH_BYTES.clone();
        mutated[0] = (byte) 0xFE;
        InvoiceMetadata different = newMetadata(mutated, CORRECTION_HASH);
        assertNotEquals(baseline, different);
    }

    @Test
    void equals_isFalseAgainstUnrelatedType() {
        InvoiceMetadata baseline = newMetadata(HASH_BYTES, CORRECTION_HASH);
        assertNotEquals("not-a-metadata", baseline);
    }

    @Test
    void equals_isTrueForSameInstance() {
        InvoiceMetadata baseline = newMetadata(HASH_BYTES, CORRECTION_HASH);
        assertEquals(baseline, baseline);
    }

    @Test
    void toString_carriesKsefNumberAndInvoiceNumber() {
        InvoiceMetadata metadata = newMetadata(HASH_BYTES, CORRECTION_HASH);
        String rendered = metadata.toString();
        assertTrue(rendered.contains(KSEF_NUMBER.value()),
                () -> "toString should reference the KSeF number: " + rendered);
        assertTrue(rendered.contains(INVOICE_NUMBER),
                () -> "toString should reference the invoice number: " + rendered);
    }

    private static InvoiceMetadata newMetadata(byte[] invoiceHash, byte[] correctionHash) {
        return new InvoiceMetadata(
                KSEF_NUMBER, INVOICE_NUMBER, ISSUE_DATE, INVOICING_DATE,
                INVOICING_DATE, INVOICING_DATE,
                null, null, null, null, null, null,
                null, null, null, null, null,
                invoiceHash, correctionHash, List.of());
    }
}
