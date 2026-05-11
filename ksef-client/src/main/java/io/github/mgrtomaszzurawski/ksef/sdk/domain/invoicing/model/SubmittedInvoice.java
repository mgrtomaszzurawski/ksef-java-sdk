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
 * Synchronous result of {@code OnlineSession.sendInvoice(Invoice)} and
 * the offline-mode variants
 * ({@code OnlineSession.sendOfflineInvoice(OfflineInvoice)},
 * {@code OnlineSession.sendTechnicalCorrection(...)}).
 *
 * <p>Carries the full terminal-state shape that consumers need:
 * KSeF number, KOD I QR PNG, KOD II QR PNG (offline path only), error
 * details, and the original {@link Invoice} embedded for downstream
 * chaining.
 *
 * <p>The SDK blocks until KSeF reaches a terminal state (Accepted or
 * Rejected) before constructing this record. On verification timeout
 * the SDK throws {@code KsefSessionPollingTimeoutException} instead;
 * the caller can re-query {@code session.invoiceStatus(referenceNumber)}
 * later if they want to retry the wait.
 *
 * @param invoice the original {@link Invoice} passed to the send call;
 *     embedded for chain-of-custody and stream fold patterns ("for each
 *     invoice, send and tell me what happened")
 * @param referenceNumber session-level invoice reference assigned by
 *     KSeF immediately after submission (always present)
 * @param status terminal session-invoice status snapshot
 *     (Accepted/Rejected/etc.) — non-null
 * @param ksefNumber populated when the invoice was accepted; empty on
 *     rejection or any non-Accepted terminal state
 * @param kodIQr KOD I (online verification) QR-code PNG bytes,
 *     populated on Accepted (online path) OR carried straight from the
 *     supplied {@code OfflineInvoice} on the offline path; empty
 *     otherwise. The verification URL embedded in the QR is the
 *     per-spec
 *     {@code https://qr-{env}.ksef.mf.gov.pl/invoice/{nip}/{date}/{hash}}
 *     link
 * @param kodIIQr KOD II (offline-certificate authenticity) QR-code PNG
 *     bytes — populated only on the offline-send path (carried from
 *     the supplied {@code OfflineInvoice}); empty on online sends
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
        Optional<byte[]> kodIIQr,
        List<String> errorDetails) {

    private static final String ERR_INVOICE_NULL = "invoice must not be null";
    private static final String ERR_REF_NULL = "referenceNumber must not be null";
    private static final String ERR_STATUS_NULL = "status must not be null";
    private static final String ERR_KSEF_NUMBER_NULL = "ksefNumber must not be null (use Optional.empty())";
    private static final String ERR_KOD_I_NULL = "kodIQr must not be null (use Optional.empty())";
    private static final String ERR_KOD_II_NULL = "kodIIQr must not be null (use Optional.empty())";
    private static final String ERR_ERROR_DETAILS_NULL = "errorDetails must not be null (use List.of())";

    /**
     * Compact constructor — defensive-copies the QR bytes. Optional and
     * List components must be supplied non-null per Sonar S2789 / S2293
     * (use {@link Optional#empty()} and {@link List#of()} for absent values).
     */
    public SubmittedInvoice {
        Objects.requireNonNull(invoice, ERR_INVOICE_NULL);
        Objects.requireNonNull(referenceNumber, ERR_REF_NULL);
        Objects.requireNonNull(status, ERR_STATUS_NULL);
        Objects.requireNonNull(ksefNumber, ERR_KSEF_NUMBER_NULL);
        Objects.requireNonNull(kodIQr, ERR_KOD_I_NULL);
        Objects.requireNonNull(kodIIQr, ERR_KOD_II_NULL);
        Objects.requireNonNull(errorDetails, ERR_ERROR_DETAILS_NULL);
        // Defensive copy at construction so a caller-supplied byte[] cannot
        // be mutated through the SubmittedInvoice. Match the Optional
        // contract — Optional<byte[]> with a fresh array when present.
        kodIQr = kodIQr.map(byte[]::clone);
        kodIIQr = kodIIQr.map(byte[]::clone);
        errorDetails = List.copyOf(errorDetails);
    }

    /**
     * Compatibility constructor — pre-PR13 5-arg form (no
     * {@code kodIIQr}) used by online-only sends. Preserved so existing
     * online-send call sites compile unchanged. The new 6th parameter
     * defaults to {@link Optional#empty()}.
     */
    public SubmittedInvoice(Invoice invoice,
                            String referenceNumber,
                            SessionInvoiceStatus status,
                            Optional<KsefNumber> ksefNumber,
                            Optional<byte[]> kodIQr,
                            List<String> errorDetails) {
        this(invoice, referenceNumber, status, ksefNumber, kodIQr,
                Optional.empty(), errorDetails);
    }

    /**
     * Returns a fresh defensive copy of the KOD I QR-code bytes so
     * callers cannot mutate the cached array through repeated access.
     * Empty when {@link #ksefNumber()} is empty (not Accepted) and the
     * invoice was sent through the online path; populated when the
     * invoice was sent via the offline path even before KSeF assigns
     * the canonical KSeF number.
     */
    @Override
    public Optional<byte[]> kodIQr() {
        return kodIQr.map(byte[]::clone);
    }

    /**
     * Returns a fresh defensive copy of the KOD II QR-code bytes so
     * callers cannot mutate the cached array through repeated access.
     * Populated only on the offline-send path; empty on online sends.
     */
    @Override
    public Optional<byte[]> kodIIQr() {
        return kodIIQr.map(byte[]::clone);
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
                && optionalBytesEqual(kodIIQr, that.kodIIQr)
                && Objects.equals(errorDetails, that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoice, referenceNumber, status, ksefNumber,
                optionalBytesHashCode(kodIQr), optionalBytesHashCode(kodIIQr),
                errorDetails);
    }

    @Override
    public String toString() {
        return "SubmittedInvoice[referenceNumber=" + referenceNumber
                + ", ksefNumber=" + ksefNumber.map(KsefNumber::value).orElse("<absent>")
                + ", kodIQr=" + kodIQr.map(bytes -> bytes.length + " bytes").orElse("<absent>")
                + ", kodIIQr=" + kodIIQr.map(bytes -> bytes.length + " bytes").orElse("<absent>")
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
