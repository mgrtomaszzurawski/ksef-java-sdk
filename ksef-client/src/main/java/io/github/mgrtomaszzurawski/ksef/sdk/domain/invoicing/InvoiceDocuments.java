/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import java.util.Objects;

/**
 * Package-private factory for {@link InvoiceDocument} anonymous wrappers
 * (unknown / custom FormCode). Same-package callers (e.g.
 * {@code ClosedSessionImpl}) call {@link #anonymousFromXml} directly;
 * cross-package SDK internals route through
 * {@code InvoiceDocumentConstructor} (reflective bridge).
 *
 * <p>R1-5: replaces the previous {@code InvoiceDocument.fromXml(...)}
 * static method that lived on the {@link InvoiceDocument} interface.
 * Interface static methods are implicitly {@code public}; this class
 * lets the factory be properly scoped to SDK internals.
 */
final class InvoiceDocuments {

    private InvoiceDocuments() { }

    /**
     * Forward-compat wrapper for unknown / custom FormCode. Returns a
     * minimal {@link InvoiceDocument} exposing only {@link InvoiceDocument#formCode()}
     * and {@link InvoiceDocument#xml()}; typed accessors are unavailable
     * because the SDK has no schema to unmarshal against.
     */
    static InvoiceDocument anonymousFromXml(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, InvoiceDocumentMessages.ERR_NULL_FORM_CODE);
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        byte[] copy = xml.clone();
        return new InvoiceDocument() {
            @Override
            public FormCode formCode() {
                return formCode;
            }

            @Override
            public byte[] xml() {
                return copy.clone();
            }

            @Override
            public String toString() {
                return "InvoiceDocument[formCode=" + formCode + ", xml=byte[" + copy.length + "]]";
            }
        };
    }
}
