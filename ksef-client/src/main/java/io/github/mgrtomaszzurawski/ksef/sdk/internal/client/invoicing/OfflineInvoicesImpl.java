/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.OfflineInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Build-only implementation of {@link OfflineInvoices}. The
 * authorisation context (QR environment, seller NIP, KOD II context,
 * issue date) is resolved from the authenticated {@code KsefClient};
 * the signing certificate comes from the configured
 * {@link OfflineSigningProvider} (default path) or is supplied
 * explicitly per call.
 *
 * @since 1.0.0
 */
public final class OfflineInvoicesImpl implements OfflineInvoices {

    private static final String ERR_NULL_INVOICE = "invoice must not be null";
    private static final String ERR_NULL_MODE = "mode must not be null";
    private static final String ERR_NULL_CERT = "certificate must not be null";
    private static final String ERR_NULL_HASH = "hashOfOriginal must not be null";
    private static final String ERR_NO_PROVIDER =
            "issue(invoice, mode) requires an OfflineSigningProvider configured via"
                    + " KsefClient.Builder.offlineSigning(...); use"
                    + " issue(invoice, mode, certificate) when the certificate is supplied per call";
    private static final String ERR_NO_CONTEXT =
            "OfflineInvoices requires an authenticated KsefClient with environment + NIP"
                    + " credentials; the bare HttpRuntime-only constructor on InvoicesImpl"
                    + " is for test scaffolding and does not expose offline issuance";

    @Nullable private final OfflineSigningProvider provider;
    @Nullable private final KsefEnvironment environment;
    @Nullable private final String sellerNip;

    public OfflineInvoicesImpl(@Nullable OfflineSigningProvider provider,
                               @Nullable KsefEnvironment environment,
                               @Nullable String sellerNip) {
        this.provider = provider;
        this.environment = environment;
        this.sellerNip = sellerNip;
    }

    @Override
    public <I extends Invoice> OfflineInvoice<I> issue(I invoice, OfflineMode mode) {
        Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
        Objects.requireNonNull(mode, ERR_NULL_MODE);
        if (provider == null) {
            throw new IllegalStateException(ERR_NO_PROVIDER);
        }
        OfflineSigningContext context = resolveContext();
        return provider.signAndPackage(invoice, mode, context);
    }

    @Override
    public <I extends Invoice> OfflineInvoice<I> issue(I invoice, OfflineMode mode, KsefCertificate certificate) {
        Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
        Objects.requireNonNull(mode, ERR_NULL_MODE);
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        OfflineSigningContext context = resolveContext();
        return OfflineInvoice.fromInvoice(invoice, certificate, mode, context);
    }

    @Override
    public <I extends Invoice> OfflineInvoice<I> issueTechnicalCorrection(I invoice,
                                                                           byte[] hashOfOriginal,
                                                                           OfflineMode mode) {
        Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
        Objects.requireNonNull(hashOfOriginal, ERR_NULL_HASH);
        Objects.requireNonNull(mode, ERR_NULL_MODE);
        if (provider == null) {
            throw new IllegalStateException(ERR_NO_PROVIDER);
        }
        // Provider builds the base OfflineInvoice (signs KOD II); we then
        // attach the hashOfCorrectedInvoice via the builder so the wire
        // send routes through SendInvoiceCommand.technicalCorrection.
        //
        // Maintenance note: the rebuild fields below must stay in sync
        // with OfflineInvoiceBuilder. If the builder gains a new required
        // field, this method silently drops it. Future cleanup: expose
        // OfflineInvoice.toBuilder() OR
        // OfflineInvoice.withHashOfCorrectedInvoice(byte[]) so the rebuild
        // copies all fields automatically. Tracked: review-iter-7 finding.
        OfflineSigningContext context = resolveContext();
        OfflineInvoice<I> base = provider.signAndPackage(invoice, mode, context);
        return OfflineInvoiceBuilder.<I>forInvoice(invoice)
                .signingCertificate(base.signingCertificate())
                .offlineMode(mode)
                .qrEnvironment(context.environment())
                .contextType(context.contextType())
                .contextValue(context.contextValue())
                .sellerNip(context.sellerNip())
                .issueDate(context.issueDate())
                .hashOfCorrectedInvoice(hashOfOriginal)
                .build();
    }

    private OfflineSigningContext resolveContext() {
        if (environment == null || sellerNip == null) {
            throw new IllegalStateException(ERR_NO_CONTEXT);
        }
        QrEnvironment qrEnvironment = QrEnvironment.fromKsefEnvironment(environment);
        return new OfflineSigningContext(
                qrEnvironment, QrContextType.NIP, sellerNip, sellerNip,
                LocalDate.now(ZoneOffset.UTC));
    }
}
