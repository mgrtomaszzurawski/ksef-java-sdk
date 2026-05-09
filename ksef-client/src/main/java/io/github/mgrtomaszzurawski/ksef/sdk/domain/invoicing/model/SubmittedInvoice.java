/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Synchronous result of {@code OnlineSession.sendInvoice(Invoice)}.
 *
 * <p>Replaces {@code SendInvoiceResult}'s single-field reference number
 * with the full terminal-state shape that consumers actually need:
 * KSeF number, KOD I QR PNG, error details, and the original
 * {@link Invoice} embedded for downstream chaining.
 *
 * <p>The SDK blocks until KSeF reaches a terminal state (Accepted or
 * Rejected) before constructing this record. On verification timeout
 * the SDK throws {@code KsefSessionPollingTimeoutException} instead;
 * the caller can re-query {@code session.invoiceStatus(referenceNumber)}
 * later if they want to retry the wait.
 *
 * @param invoice the original {@link Invoice} passed to
 *     {@code sendInvoice}; embedded for chain-of-custody and stream
 *     fold patterns ("for each invoice, send and tell me what
 *     happened")
 * @param referenceNumber session-level invoice reference assigned by
 *     KSeF immediately after submission (always present)
 * @param status terminal session-invoice status snapshot
 *     (Accepted/Rejected/etc.) — non-null
 * @param ksefNumber populated when the invoice was accepted; empty on
 *     rejection or any non-Accepted terminal state
 * @param kodIQr KOD I (online verification) QR-code PNG bytes,
 *     populated on Accepted; empty otherwise. The verification URL
 *     embedded in the QR is the per-spec
 *     {@code https://qr-{env}.ksef.mf.gov.pl/invoice/{nip}/{date}/{hash}}
 *     link
 * @param errorDetails human-readable error messages reported by KSeF
 *     on rejection; empty list on success
 *
 * @since 1.0.0
 */
public record SubmittedInvoice(
        Invoice invoice,
        String referenceNumber,
        SessionInvoiceStatus status,
        Optional<KsefNumber> ksefNumber,
        Optional<byte[]> kodIQr,
        List<String> errorDetails) {

    private static final String ERR_INVOICE_NULL = "invoice must not be null";
    private static final String ERR_REF_NULL = "referenceNumber must not be null";
    private static final String ERR_STATUS_NULL = "status must not be null";

    /**
     * Compact constructor — normalises the optional/list inputs and
     * defensive-copies the QR bytes.
     */
    public SubmittedInvoice {
        Objects.requireNonNull(invoice, ERR_INVOICE_NULL);
        Objects.requireNonNull(referenceNumber, ERR_REF_NULL);
        Objects.requireNonNull(status, ERR_STATUS_NULL);
        ksefNumber = ksefNumber == null ? Optional.empty() : ksefNumber;
        // Defensive copy at construction so a caller-supplied byte[] cannot
        // be mutated through the SubmittedInvoice. Match the Optional
        // contract — Optional<byte[]> with a fresh array when present.
        kodIQr = kodIQr == null
                ? Optional.empty()
                : kodIQr.map(byte[]::clone);
        errorDetails = errorDetails == null ? List.of() : List.copyOf(errorDetails);
    }

    /**
     * Returns a fresh defensive copy of the QR-code bytes so callers
     * cannot mutate the cached array through repeated access. Empty
     * when {@link #ksefNumber()} is empty (not Accepted).
     */
    @Override
    public Optional<byte[]> kodIQr() {
        return kodIQr.map(byte[]::clone);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SubmittedInvoice that)) {
            return false;
        }
        return Objects.equals(invoice, that.invoice)
                && Objects.equals(referenceNumber, that.referenceNumber)
                && Objects.equals(status, that.status)
                && Objects.equals(ksefNumber, that.ksefNumber)
                && optionalBytesEqual(kodIQr, that.kodIQr)
                && Objects.equals(errorDetails, that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoice, referenceNumber, status, ksefNumber,
                optionalBytesHashCode(kodIQr), errorDetails);
    }

    @Override
    public String toString() {
        return "SubmittedInvoice[referenceNumber=" + referenceNumber
                + ", ksefNumber=" + ksefNumber.map(KsefNumber::value).orElse("<absent>")
                + ", kodIQr=" + kodIQr.map(bytes -> bytes.length + " bytes").orElse("<absent>")
                + ", errorDetails=" + errorDetails.size() + " entries"
                + ", statusCode="
                + (status.status() == null ? "<unknown>" : Integer.toString(status.status().code()))
                + "]";
    }

    private static boolean optionalBytesEqual(Optional<byte[]> first, Optional<byte[]> second) {
        if (first.isEmpty() && second.isEmpty()) {
            return true;
        }
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        return Arrays.equals(first.get(), second.get());
    }

    private static int optionalBytesHashCode(Optional<byte[]> bytes) {
        return bytes.map(Arrays::hashCode).orElse(0);
    }
}
