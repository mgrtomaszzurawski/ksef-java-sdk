/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * One UPO (Urzędowe Poświadczenie Odbioru — official KSeF receipt) returned
 * for an accepted invoice in a batch.
 *
 * <p>Placeholder structure for the {@code BatchResult.cleared} list. PR15 will
 * widen the per-element type to {@code ClearedInvoice}, embedding the full
 * {@code SubmittedInvoice} chain. Until then, {@link UpoEntry} carries only
 * the per-invoice reference number and the raw UPO XML bytes.
 *
 * @param referenceNumber the KSeF invoice reference number
 *     (server-assigned identifier tying this entry to the input invoice
 *     ordinal; preserved across the whole flow)
 * @param xmlBytes raw UPO XML bytes (immutable defensive copy on accessor)
 *
 * @since 1.0.0
 */
public record UpoEntry(String referenceNumber, byte[] xmlBytes) {

    private static final String ERR_REF_NULL = "referenceNumber must not be null";
    private static final String ERR_XML_NULL = "xmlBytes must not be null";

    public UpoEntry {
        Objects.requireNonNull(referenceNumber, ERR_REF_NULL);
        Objects.requireNonNull(xmlBytes, ERR_XML_NULL);
        xmlBytes = xmlBytes.clone();
    }

    /**
     * Defensive copy of the UPO XML bytes — every accessor call clones so the
     * canonical content cannot be mutated by callers.
     */
    @Override
    public byte[] xmlBytes() {
        return xmlBytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UpoEntry that)) {
            return false;
        }
        return Objects.equals(referenceNumber, that.referenceNumber)
                && Arrays.equals(xmlBytes, that.xmlBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceNumber, Arrays.hashCode(xmlBytes));
    }

    @Override
    public String toString() {
        return "UpoEntry[referenceNumber=" + referenceNumber
                + ", xmlBytes=byte[" + xmlBytes.length + "]]";
    }
}
