/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import java.util.Arrays;
import java.util.Objects;

/**
 * The mode in which an invoice is sent to KSeF within a session.
 *
 * <p>KSeF defines three send modes (see
 * {@code ksef-docs/sesja-interaktywna.md},
 * {@code ksef-docs/offline/automatyczne-okreslanie-trybu-offline.md} and
 * {@code ksef-docs/offline/korekta-techniczna.md}):
 *
 * <ul>
 *   <li>{@link Normal} — standard online or batch send. The invoice XML
 *       is encrypted with the session AES key and submitted with
 *       {@code offlineMode=false}.</li>
 *   <li>{@link Offline} — invoice issued during an offline window
 *       (offline24, awaria, niedostępność). Same wire shape as
 *       {@link Normal} except {@code offlineMode=true} so the server
 *       knows to apply offline-issuance rules.</li>
 *   <li>{@link TechnicalCorrection} — a "korekta techniczna" (technical
 *       correction) replacing an earlier invoice. The request must
 *       include {@code hashOfCorrectedInvoice} (SHA-256 of the original
 *       invoice's XML content) and is implicitly offline at the wire
 *       level. Permitted only within an interactive online session per
 *       spec (REQ-OFFLINE-005).</li>
 * </ul>
 *
 * <p>Use the static factory methods {@link #normal(byte[])},
 * {@link #offline(byte[])} and
 * {@link #technicalCorrection(byte[], byte[])} to obtain instances.
 *
 * <p>Spec citation: REQ-OFFLINE-003 in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}.
 *
 * @since 1.0.0
 */
public sealed interface SendInvoiceCommand
        permits SendInvoiceCommand.Normal, SendInvoiceCommand.Offline, SendInvoiceCommand.TechnicalCorrection {

    /** Shared error message for nested-record null checks. */
    String ERR_INVOICE_XML_NULL = "invoiceXml must not be null";
    /** Shared toString() suffix used by the byte[]-summarising overrides. */
    String BYTES_TOSTRING_SUFFIX = " bytes]";

    /**
     * The invoice XML content to send. Returned as a defensive copy.
     */
    byte[] invoiceXml();

    /**
     * Construct a normal (non-correction, online) send command.
     *
     * @param invoiceXml the invoice XML bytes (unencrypted)
     */
    static SendInvoiceCommand normal(byte[] invoiceXml) {
        return new Normal(invoiceXml);
    }

    /**
     * Construct an offline-mode send command (Codex 2026-05-05 F1).
     * Use this when the invoice was issued during an offline window
     * (offline24, awaria, niedostępność) and KSeF must apply
     * offline-issuance rules at acceptance time.
     *
     * @param invoiceXml the invoice XML bytes (unencrypted)
     */
    static SendInvoiceCommand offline(byte[] invoiceXml) {
        return new Offline(invoiceXml);
    }

    /**
     * Construct a technical-correction send command.
     *
     * @param invoiceXml the corrected (replacement) invoice XML bytes
     * @param hashOfCorrectedInvoice SHA-256 of the original invoice's XML content
     */
    static SendInvoiceCommand technicalCorrection(byte[] invoiceXml, byte[] hashOfCorrectedInvoice) {
        return new TechnicalCorrection(invoiceXml, hashOfCorrectedInvoice);
    }

    /**
     * Standard online or batch invoice send.
     */
    record Normal(byte[] invoiceXml) implements SendInvoiceCommand {

        public Normal {
            Objects.requireNonNull(invoiceXml, ERR_INVOICE_XML_NULL);
            invoiceXml = invoiceXml.clone();
        }

        @Override
        public byte[] invoiceXml() {
            return invoiceXml.clone();
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || (o instanceof Normal other && Arrays.equals(invoiceXml, other.invoiceXml));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(invoiceXml);
        }

        @Override
        public String toString() {
            return "Normal[invoiceXml=" + invoiceXml.length + BYTES_TOSTRING_SUFFIX;
        }
    }

    /**
     * Offline-mode invoice send. Same wire shape as {@link Normal} except
     * {@code offlineMode=true}. Used when the invoice was issued during
     * an offline window (offline24, awaria, niedostępność). Codex
     * 2026-05-05 F1.
     */
    record Offline(byte[] invoiceXml) implements SendInvoiceCommand {

        public Offline {
            Objects.requireNonNull(invoiceXml, ERR_INVOICE_XML_NULL);
            invoiceXml = invoiceXml.clone();
        }

        @Override
        public byte[] invoiceXml() {
            return invoiceXml.clone();
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || (o instanceof Offline other && Arrays.equals(invoiceXml, other.invoiceXml));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(invoiceXml);
        }

        @Override
        public String toString() {
            return "Offline[invoiceXml=" + invoiceXml.length + BYTES_TOSTRING_SUFFIX;
        }
    }

    /**
     * Technical-correction (korekta techniczna) send. The
     * {@code hashOfCorrectedInvoice} field is required by KSeF and
     * carries the SHA-256 of the original invoice's XML content.
     *
     * <p>Setting this implies offline mode at the wire level
     * (REQ-OFFLINE-004). The session-type validation (online-only,
     * REQ-OFFLINE-005) is enforced by {@code KsefSession} when this
     * command is dispatched.
     */
    record TechnicalCorrection(byte[] invoiceXml, byte[] hashOfCorrectedInvoice) implements SendInvoiceCommand {

        /** SHA-256 length in bytes — KSeF requires the corrected-invoice hash to be exactly 32 bytes. */
        private static final int SHA256_LENGTH_BYTES = 32;

        public TechnicalCorrection {
            Objects.requireNonNull(invoiceXml, ERR_INVOICE_XML_NULL);
            Objects.requireNonNull(hashOfCorrectedInvoice, "hashOfCorrectedInvoice must not be null");
            if (hashOfCorrectedInvoice.length != SHA256_LENGTH_BYTES) {
                throw new IllegalArgumentException(
                        "hashOfCorrectedInvoice must be exactly " + SHA256_LENGTH_BYTES
                                + " bytes (SHA-256), got " + hashOfCorrectedInvoice.length);
            }
            invoiceXml = invoiceXml.clone();
            hashOfCorrectedInvoice = hashOfCorrectedInvoice.clone();
        }

        @Override
        public byte[] invoiceXml() {
            return invoiceXml.clone();
        }

        @Override
        public byte[] hashOfCorrectedInvoice() {
            return hashOfCorrectedInvoice.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TechnicalCorrection other)) {
                return false;
            }
            return Arrays.equals(invoiceXml, other.invoiceXml)
                    && Arrays.equals(hashOfCorrectedInvoice, other.hashOfCorrectedInvoice);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(invoiceXml), Arrays.hashCode(hashOfCorrectedInvoice));
        }

        @Override
        public String toString() {
            return "TechnicalCorrection[invoiceXml=" + invoiceXml.length + " bytes"
                    + ", hashOfCorrectedInvoice=" + hashOfCorrectedInvoice.length + BYTES_TOSTRING_SUFFIX;
        }
    }
}
