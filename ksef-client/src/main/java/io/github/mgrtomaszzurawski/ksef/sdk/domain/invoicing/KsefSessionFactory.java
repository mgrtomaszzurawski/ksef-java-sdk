/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import java.net.http.HttpClient;
import java.util.List;

/**
 * Internal construction bridge for {@link KsefSession} and {@link KsefBatchSession}.
 *
 * <p>Both classes have package-private constructors taking internal-package
 * types ({@link SessionClient}, {@link BatchPackageBuilder.BatchPackage});
 * those constructors are not part of the public API contract. {@code KsefClient}
 * lives in a different package, so it cannot reach the constructors directly —
 * this same-package bridge is the only legal construction path from the SDK
 * facade.
 *
 * <p>Codex round-9 fresh review H3 — eliminates the prior public constructors
 * with internal-typed parameters, which leaked construction details into the
 * binary/Javadoc surface even though JPMS made the parameter types
 * unreachable from consumers. The factory's methods still reference internal
 * parameter types (necessary for the cross-package call), but consolidate
 * the surface area into a single, clearly-named {@code @apiNote Internal}
 * entry point per class.
 *
 * @apiNote Internal — must not be called from consumer code; reserved for
 *     {@link io.github.mgrtomaszzurawski.ksef.sdk.KsefClient} and other
 *     in-module SDK internals.
 */
public final class KsefSessionFactory {

    private KsefSessionFactory() { }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefSession newOnlineSession(SessionClient sessionClient,
                                                 String referenceNumber,
                                                 byte[] aesKey,
                                                 byte[] initVector) {
        return new KsefSession(sessionClient, referenceNumber, aesKey, initVector);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests) {
        return new KsefBatchSession(sessionClient, referenceNumber, partUploadRequests);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    HttpClient httpClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests,
                                                    BatchPackageBuilder.BatchPackage batchPackage) {
        return new KsefBatchSession(sessionClient, httpClient, referenceNumber,
                partUploadRequests, batchPackage);
    }

    /**
     * @apiNote Internal — overload with injectable nano-time source for
     * upload-budget tests; see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    HttpClient httpClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests,
                                                    BatchPackageBuilder.BatchPackage batchPackage,
                                                    java.util.function.LongSupplier nanoTimeSource) {
        return new KsefBatchSession(sessionClient, httpClient, referenceNumber,
                partUploadRequests, batchPackage, nanoTimeSource);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static PreparedInvoiceExport newPreparedExport(InvoiceClient invoices,
                                                            HttpClient httpClient,
                                                            String referenceNumber,
                                                            byte[] aesKey,
                                                            byte[] initVector) {
        return new PreparedInvoiceExport(invoices, httpClient, referenceNumber, aesKey, initVector);
    }
}
