/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceBatch;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.InvoiceSessions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceSync;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import java.security.PublicKey;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Coordinator implementation of {@link Invoices}. Instantiates the five
 * sub-area implementations once at construction and returns them through
 * the {@code archive()}, {@code sessions()}, {@code offline()},
 * {@code export()}, and {@code sync()} accessors. The batch flow is
 * reached via {@code sessions().batch()} (R2-4 B).
 *
 * <p>The single-arg constructor produces a coordinator whose
 * session-aware sub-areas ({@code sessions()}, including its nested
 * {@code batch()}, {@code offline()}, {@code clearedFromArchive()}) throw
 * {@link IllegalStateException} on use — wire the full runtime through
 * the multi-arg constructor for those code paths.
 *
 * @since 1.0.0
 */
public final class InvoicesImpl implements Invoices {

    private static final java.time.Duration DEFAULT_INVOICE_VERIFICATION_TIMEOUT = java.time.Duration.ofSeconds(60);

    private static final String ERR_OPEN_SESSION_REQUIRES_FULL_RUNTIME =
            "InvoiceSessions requires the full Invoices runtime — instantiate via the multi-arg constructor";
    private static final String ERR_SUBMIT_BATCH_REQUIRES_FULL_RUNTIME =
            "InvoiceBatch requires the full Invoices runtime — instantiate via the multi-arg constructor";
    private static final String ERR_OFFLINE_REQUIRES_FULL_RUNTIME =
            "OfflineInvoices requires the full Invoices runtime — instantiate via the multi-arg constructor";

    private final InvoiceArchive archive;
    private final InvoiceExport export;
    private final InvoiceSync sync;
    private final InvoiceSessions sessions;
    private final OfflineInvoices offline;

    public InvoicesImpl(HttpRuntime runtime) {
        this(runtime, null, null, null, DEFAULT_INVOICE_VERIFICATION_TIMEOUT, null, null, KsefInvoiceTypes.builtinsOnly());
    }

    public InvoicesImpl(HttpRuntime runtime,
                        @Nullable SessionClient sessionClient,
                        @Nullable KsefEnvironment environment,
                        @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver) {
        this(runtime, sessionClient, environment, publicKeyResolver, DEFAULT_INVOICE_VERIFICATION_TIMEOUT,
                null, null, KsefInvoiceTypes.builtinsOnly());
    }

    public InvoicesImpl(HttpRuntime runtime,
                        @Nullable SessionClient sessionClient,
                        @Nullable KsefEnvironment environment,
                        @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver,
                        java.time.Duration invoiceVerificationTimeout) {
        this(runtime, sessionClient, environment, publicKeyResolver, invoiceVerificationTimeout,
                null, null, KsefInvoiceTypes.builtinsOnly());
    }

    public InvoicesImpl(HttpRuntime runtime,
                        @Nullable SessionClient sessionClient,
                        @Nullable KsefEnvironment environment,
                        @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver,
                        java.time.Duration invoiceVerificationTimeout,
                        @Nullable OfflineSigningProvider offlineSigningProvider,
                        @Nullable String sellerNip) {
        this(runtime, sessionClient, environment, publicKeyResolver, invoiceVerificationTimeout,
                offlineSigningProvider, sellerNip, KsefInvoiceTypes.builtinsOnly());
    }

    // 8 collaborators: full Invoices coordinator needs transport + session
    // client + env + public-key resolver + verification timeout + offline
    // signing wiring + custom invoice-type registry. Same reasoning as the
    // sub-impl: bundling into a "config record" only renames args.
    @SuppressWarnings("java:S107")
    public InvoicesImpl(HttpRuntime runtime,
                        @Nullable SessionClient sessionClient,
                        @Nullable KsefEnvironment environment,
                        @Nullable Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver,
                        java.time.Duration invoiceVerificationTimeout,
                        @Nullable OfflineSigningProvider offlineSigningProvider,
                        @Nullable String sellerNip,
                        KsefInvoiceTypes invoiceTypes) {
        Objects.requireNonNull(runtime, "runtime must not be null");
        Objects.requireNonNull(invoiceVerificationTimeout, "invoiceVerificationTimeout must not be null");
        Objects.requireNonNull(invoiceTypes, "invoiceTypes must not be null");
        boolean sessionsAvailable = sessionClient != null && environment != null && publicKeyResolver != null;
        this.archive = new InvoiceArchiveImpl(runtime, sessionClient, invoiceTypes);
        this.export = new InvoiceExportImpl(runtime);
        this.sync = new InvoiceSyncImpl(runtime, this.export, invoiceTypes);
        InvoiceBatch batch = sessionsAvailable
                ? new InvoiceBatchImpl(sessionClient, runtime.httpClient(), environment, publicKeyResolver)
                : new UnavailableBatch();
        this.sessions = sessionsAvailable
                ? new InvoiceSessionsImpl(sessionClient, environment, publicKeyResolver,
                        invoiceVerificationTimeout, offlineSigningProvider, sellerNip, batch, invoiceTypes)
                : new UnavailableSessions(batch);
        this.offline = sessionsAvailable
                ? new OfflineInvoicesImpl(offlineSigningProvider, environment, sellerNip)
                : new UnavailableOffline();
    }

    @Override
    public InvoiceArchive archive() {
        return archive;
    }

    @Override
    public InvoiceSessions sessions() {
        return sessions;
    }

    @Override
    public OfflineInvoices offline() {
        return offline;
    }

    @Override
    public InvoiceExport export() {
        return export;
    }

    @Override
    public InvoiceSync sync() {
        return sync;
    }

    private record UnavailableSessions(InvoiceBatch batch) implements InvoiceSessions {

        @Override
        public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.OnlineSession online(
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode formCode) {
            throw new IllegalStateException(ERR_OPEN_SESSION_REQUIRES_FULL_RUNTIME);
        }

        @Override
        public java.util.stream.Stream<
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionListItem> stream(
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryRequest filter) {
            throw new IllegalStateException(ERR_OPEN_SESSION_REQUIRES_FULL_RUNTIME);
        }
    }

    private static final class UnavailableBatch implements InvoiceBatch {
        @Override
        public <I extends io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice>
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult<I> submit(
                java.util.List<I> invoices,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions options) {
            throw new IllegalStateException(ERR_SUBMIT_BATCH_REQUIRES_FULL_RUNTIME);
        }

        @Override
        public io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult<
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice> submitFromFiles(
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode formCode,
                java.util.List<java.nio.file.Path> files,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions options) {
            throw new IllegalStateException(ERR_SUBMIT_BATCH_REQUIRES_FULL_RUNTIME);
        }
    }

    private static final class UnavailableOffline implements OfflineInvoices {
        @Override
        public <I extends io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice>
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice<I> issue(
                I invoice, io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode mode) {
            throw new IllegalStateException(ERR_OFFLINE_REQUIRES_FULL_RUNTIME);
        }

        @Override
        public <I extends io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice>
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice<I> issue(
                I invoice, io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode mode,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate certificate) {
            throw new IllegalStateException(ERR_OFFLINE_REQUIRES_FULL_RUNTIME);
        }

        @Override
        public <I extends io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice>
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice<I> issueTechnicalCorrection(
                I invoice, byte[] hashOfOriginal,
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode mode) {
            throw new IllegalStateException(ERR_OFFLINE_REQUIRES_FULL_RUNTIME);
        }
    }
}
