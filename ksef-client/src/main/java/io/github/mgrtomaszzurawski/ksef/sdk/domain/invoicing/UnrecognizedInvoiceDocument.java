/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import java.util.Arrays;
import java.util.Objects;

/**
 * Typed fallback returned by the read-side flow when KSeF returns an
 * invoice whose {@link FormCode} matches neither the SDK's built-in
 * schemas (FA2/FA3/PEF/PEF_KOR) nor any custom type the consumer
 * registered via
 * {@code KsefInvoiceTypes.builder().register(MyCustomInvoiceDocument.class)}.
 *
 * <p>The wrapper exposes only {@link #formCode()} and {@link #xml()};
 * schema-specific accessors are unavailable because the SDK has no
 * binding to unmarshal against. Pattern-match consumers should land
 * here on the fallback arm:
 *
 * <pre>{@code
 * switch (doc) {
 *     case Fa3InvoiceDocument fa3 -> processFa3(fa3);
 *     case MyCustomInvoiceDocument my -> processCustom(my);
 *     case UnrecognizedInvoiceDocument unknown ->
 *             logAndStoreRaw(unknown.formCode(), unknown.xml());
 *     default -> throw new IllegalStateException("unhandled InvoiceDocument: " + doc);
 * }
 * }</pre>
 *
 * <p>Equality is structural ({@link Arrays#equals(byte[], byte[])} on
 * the XML bytes); {@link #toString} reports the form code and byte
 * count to aid diagnostics.
 *
 * @param formCode the schema identifier the server reported on the
 *     invoice (non-null)
 * @param xml the raw XML bytes the SDK fetched (non-null, defensive
 *     copy on construction and on every {@link #xml()} call)
 *
 * @since 1.0.0
 */
public record UnrecognizedInvoiceDocument(FormCode formCode, byte[] xml) implements InvoiceDocument {

    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_NULL_XML = "xml must not be null";

    public UnrecognizedInvoiceDocument {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        Objects.requireNonNull(xml, ERR_NULL_XML);
        xml = xml.clone();
    }

    @Override
    public byte[] xml() {
        return xml.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof UnrecognizedInvoiceDocument that
                && Objects.equals(formCode, that.formCode)
                && Arrays.equals(xml, that.xml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formCode, Arrays.hashCode(xml));
    }

    @Override
    public String toString() {
        return "UnrecognizedInvoiceDocument[formCode=" + formCode
                + ", xml=byte[" + xml.length + "]]";
    }
}
