/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import java.util.Objects;

/**
 * Package-private factory for {@link InvoiceDocument} unrecognized
 * wrappers (unknown / custom FormCode where the SDK has no schema
 * binding). Same-package callers (e.g. {@code ClosedSessionImpl} on
 * the UPO-only placeholder path) call {@link #anonymousFromXml}
 * directly; cross-package SDK internals route through
 * {@code InvoiceDocumentConstructor.newAnonymousDocument} which has
 * the same behaviour.
 *
 * <p>R2-6 ext: both paths now return an
 * {@link UnrecognizedInvoiceDocument} (typed record) so consumers
 * pattern-match against a named type rather than an anonymous
 * implementation. The method name {@code anonymousFromXml} is kept
 * for source compatibility with the call sites.
 */
final class InvoiceDocuments {

    private InvoiceDocuments() { }

    /**
     * Build a typed unrecognised-form wrapper. Defers to
     * {@link UnrecognizedInvoiceDocument}'s compact constructor for
     * the defensive copy + null checks.
     */
    static InvoiceDocument anonymousFromXml(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, InvoiceDocumentMessages.ERR_NULL_FORM_CODE);
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        return new UnrecognizedInvoiceDocument(formCode, xml);
    }
}
