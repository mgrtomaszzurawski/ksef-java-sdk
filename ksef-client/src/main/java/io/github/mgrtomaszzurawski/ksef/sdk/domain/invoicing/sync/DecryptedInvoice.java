/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.UnrecognizedInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * One decrypted invoice produced by {@code client.invoices().sync().asStream(...)}.
 * Carries the server's invoice metadata, the typed
 * {@link InvoiceDocument} the SDK constructed from the decrypted XML,
 * and an optional file path when the consumer's
 * {@link IncrementalSyncPlan} requested file-output mode.
 *
 * <p>The {@link #document()} field is typed for the SDK's built-in
 * schemas (FA2/FA3/PEF/PEF_KOR) and for any custom type registered via
 * {@code KsefClient.Builder.invoiceTypes(KsefInvoiceTypes)}; otherwise
 * it is an {@link UnrecognizedInvoiceDocument} fallback. Pattern-match
 * on the concrete type when schema-specific processing is needed; fall
 * back to {@link #xml()} on the unrecognised arm.
 *
 * <pre>{@code
 * try (Stream<DecryptedInvoice> stream = client.invoices().sync().asStream(plan, store)) {
 *     stream.forEach(decrypted -> {
 *         switch (decrypted.document()) {
 *             case io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa3InvoiceDocument fa3 ->
 *                 processFa3(fa3);
 *             case MyCustomInvoiceDocument custom -> processCustom(custom);
 *             case UnrecognizedInvoiceDocument unknown ->
 *                 logAndStoreRaw(unknown.formCode(), unknown.xml());
 *             default -> throw new IllegalStateException("unhandled: " + decrypted.document());
 *         }
 *     });
 * }
 * }</pre>
 *
 * <p>{@code xmlPath} is empty when the consumer requested in-memory
 * mode (no spilling to disk).
 *
 * @param metadata invoice metadata returned by the sync query
 *     (carries the KSeF number, dates, party identifiers, form code)
 * @param document typed read-side invoice document constructed from the
 *     decrypted XML (one of the built-in subtypes, a registered custom
 *     type, or {@link UnrecognizedInvoiceDocument})
 * @param xmlPath on-disk location when the plan requested file-output;
 *     empty otherwise
 *
 * @since 1.0.0
 */
public record DecryptedInvoice(
        InvoiceMetadata metadata,
        InvoiceDocument document,
        Optional<Path> xmlPath) {

    private static final String ERR_NULL_METADATA = "metadata must not be null";
    private static final String ERR_NULL_DOCUMENT = "document must not be null";
    private static final String ERR_NULL_PATH = "xmlPath must not be null";

    public DecryptedInvoice {
        Objects.requireNonNull(metadata, ERR_NULL_METADATA);
        Objects.requireNonNull(document, ERR_NULL_DOCUMENT);
        Objects.requireNonNull(xmlPath, ERR_NULL_PATH);
    }

    /**
     * Convenience: KSeF number from the metadata. Equivalent to
     * {@code metadata().ksefNumber()}.
     */
    public KsefNumber ksefNumber() {
        return metadata.ksefNumber();
    }

    /**
     * Convenience: form code from the document. Equivalent to
     * {@code document().formCode()}. Matches metadata's form code in
     * the steady state — both come from the same server payload.
     */
    public FormCode formCode() {
        return document.formCode();
    }

    /**
     * Convenience: decrypted XML bytes. Delegates to
     * {@link InvoiceDocument#xml()} on the typed document (defensive
     * copy on every call per the document's contract).
     */
    public byte[] xml() {
        return document.xml();
    }
}
