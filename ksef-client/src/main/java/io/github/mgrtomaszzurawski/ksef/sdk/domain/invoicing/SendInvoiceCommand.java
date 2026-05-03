/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import java.util.Arrays;
import java.util.Objects;

/**
 * The mode in which an invoice is sent to KSeF within a session.
 *
 * <p>KSeF defines two send modes (see
 * {@code ksef-docs/sesja-interaktywna.md} and
 * {@code ksef-docs/offline/korekta-techniczna.md}):
 *
 * <ul>
 *   <li>{@link Normal} — standard online or batch send. The invoice XML
 *       is encrypted with the session AES key and submitted.</li>
 *   <li>{@link TechnicalCorrection} — a "korekta techniczna" (technical
 *       correction) replacing an earlier invoice. The request must
 *       include {@code hashOfCorrectedInvoice} (SHA-256 of the original
 *       invoice's XML content) and must be sent in offline mode.
 *       Permitted only within an interactive online session per spec
 *       (REQ-OFFLINE-005).</li>
 * </ul>
 *
 * <p>Use the static factory methods {@link #normal(byte[])} and
 * {@link #technicalCorrection(byte[], byte[])} to obtain instances.
 *
 * <p>Spec citation: REQ-OFFLINE-003 in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}.
 */
public sealed interface SendInvoiceCommand
        permits SendInvoiceCommand.Normal, SendInvoiceCommand.TechnicalCorrection {

    /**
     * The invoice XML content to send. Returned as a defensive copy.
     */
    byte[] invoiceXml();

    /**
     * Construct a normal (non-correction) send command.
     *
     * @param invoiceXml the invoice XML bytes (unencrypted)
     */
    static SendInvoiceCommand normal(byte[] invoiceXml) {
        return new Normal(invoiceXml);
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
            Objects.requireNonNull(invoiceXml, "invoiceXml must not be null");
            invoiceXml = invoiceXml.clone();
        }

        @Override
        public byte[] invoiceXml() {
            return invoiceXml.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Normal other)) {
                return false;
            }
            return Arrays.equals(invoiceXml, other.invoiceXml);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(invoiceXml);
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

        public TechnicalCorrection {
            Objects.requireNonNull(invoiceXml, "invoiceXml must not be null");
            Objects.requireNonNull(hashOfCorrectedInvoice, "hashOfCorrectedInvoice must not be null");
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
    }
}
