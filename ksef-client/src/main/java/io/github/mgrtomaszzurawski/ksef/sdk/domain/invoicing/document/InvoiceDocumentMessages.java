/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

/**
 * Shared null-check messages for {@link InvoiceDocument} and the four
 * typed read-side document classes. Keeps each call site consistent and
 * satisfies the project rule "string literals must be private static
 * final constants" without duplicating the same string across each
 * Document class.
 */
final class InvoiceDocumentMessages {

    static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    static final String ERR_NULL_XML = "xml must not be null";
    static final String ERR_NULL_FAKTURA = "faktura must not be null";
    static final String ERR_NULL_INVOICE = "invoice must not be null";
    static final String ERR_NULL_CREDIT_NOTE = "creditNote must not be null";

    private InvoiceDocumentMessages() {
    }
}
