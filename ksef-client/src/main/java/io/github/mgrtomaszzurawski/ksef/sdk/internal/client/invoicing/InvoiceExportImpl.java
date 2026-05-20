/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.net.http.HttpClient;
import java.security.PublicKey;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport.requireSafePathSegment;

/**
 * Invoice-export implementation of {@link InvoiceExport}.
 *
 * @since 1.0.0
 */
public final class InvoiceExportImpl implements InvoiceExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceExportImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String LOG_CALL_REF = "→ {} ref={}";

    private static final String PATH_EXPORTS = ApiPaths.INVOICES + "/exports";
    private static final String PATH_EXPORT_STATUS = ApiPaths.INVOICES + "/exports/";

    private static final String OP_EXPORT_STATUS = "getExportStatus";
    private static final String OP_PREPARE_EXPORT = "prepareInvoiceExport";

    private static final String ERR_NULL_QUERY = "query must not be null";
    private static final String ERR_NO_SYMMETRIC_KEY_CERT =
            "No KSeF public key found for SYMMETRIC_KEY_ENCRYPTION usage";

    private final HttpSupport http;
    private final SecurityClient securityClient;
    private final HttpClient httpClient;

    public InvoiceExportImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.securityClient = new SecurityClient(runtime);
        this.httpClient = runtime.httpClient();
    }

    @Override
    public PreparedInvoiceExport prepare(InvoiceQueryRequest query, ExportScope scope) {
        LOGGER.debug(LOG_CALL, OP_PREPARE_EXPORT);
        Objects.requireNonNull(query, ERR_NULL_QUERY);

        PublicKey symmetricKey = securityClient.getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().contains(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION))
                .findFirst()
                .map(PublicKeyCertificate::publicKey)
                .orElseThrow(() -> new IllegalStateException(ERR_NO_SYMMETRIC_KEY_CERT));

        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, symmetricKey);

        boolean onlyMetadata = scope == ExportScope.METADATA_ONLY;
        InvoiceExportRequest request = new InvoiceExportRequest(
                encryptedKey, initVector, onlyMetadata, query);
        InvoiceExportRequestRaw rawRequest = InvoicingRequestMappers.toInvoiceExportRequestRaw(request);
        String token = http.requireToken();
        ExportInvoicesResponseRaw rawValue = http.postJsonAuthenticated(PATH_EXPORTS, rawRequest, token,
                ExportInvoicesResponseRaw.class, OP_PREPARE_EXPORT);
        ExportInvoicesResult result = InvoicingMappers.toExportInvoicesResult(rawValue);
        return SessionHandleConstructor.newPreparedExport(
                this, httpClient, result.referenceNumber(), aesKey, initVector);
    }

    @Override
    public InvoiceExportStatus getStatus(String referenceNumber) {
        LOGGER.debug(LOG_CALL_REF, OP_EXPORT_STATUS, referenceNumber);
        requireSafePathSegment(referenceNumber);
        String token = http.requireToken();
        InvoiceExportStatusResponseRaw rawValue = http.getAuthenticated(PATH_EXPORT_STATUS + referenceNumber, token,
                InvoiceExportStatusResponseRaw.class, OP_EXPORT_STATUS);
        return InvoicingMappers.toInvoiceExportStatus(rawValue);
    }
}
