/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.InvoiceClient;

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_BACKOFF_MULTIPLIER;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_INITIAL_DELAY_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_TIMEOUT_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner for InvoiceClient operations. queryMetadata works in AUTH_SAFE and FULL modes.
 * exportInvoices and getByKsefNumber require FULL mode (need invoice refs from SessionRunner).
 */
public final class InvoiceRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceRunner.class);
    private static final String NAME = "invoice";
    private static final String OP_QUERY_METADATA = "queryMetadata";
    private static final String OP_EXPORT = "exportInvoices";
    private static final String OP_EXPORT_STATUS = "getExportStatus";
    private static final String OP_GET_BY_KSEF = "getByKsefNumber";
    private static final String SKIP_NOT_FULL = "FULL mode only (needs invoice refs)";
    private static final String SKIP_NO_KSEF_NUMBER = "no KSeF number available from SessionRunner";
    private static final String SKIP_NO_EXPORT_REF = "export not started";
    private static final int EXPORT_STATUS_OK = 200;
    private static final int EXPORT_POLL_MAX_DELAY_MS = 10000;
    private static final int QUERY_DATE_RANGE_DAYS = 30;
    private static final String ERR_NO_ENCRYPTION_CERT = "No SymmetricKeyEncryption certificate found";
    private static final String CERT_TYPE_X509 = "X.509";
    private static final String ERR_KEY_EXTRACT = "Failed to extract public key";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // queryMetadata — works in AUTH_SAFE and FULL
        runQueryMetadata(context, results);

        // Export and getByKsefNumber — FULL only
        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_EXPORT, SKIP_NOT_FULL));
            results.add(RunResult.skip(NAME, OP_EXPORT_STATUS, SKIP_NOT_FULL));
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF, SKIP_NOT_FULL));
            return results;
        }

        // exportInvoices
        String exportRef = runExportInvoices(context, results);

        // getExportStatus (poll until ready)
        if (exportRef != null) {
            pollExportStatus(context, exportRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_EXPORT_STATUS, SKIP_NO_EXPORT_REF));
        }

        // getByKsefNumber — needs invoiceKsefNumber from SessionRunner
        String ksefNumber = context.invoiceKsefNumber();
        if (ksefNumber != null) {
            runGetByKsefNumber(context, ksefNumber, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF, SKIP_NO_KSEF_NUMBER));
        }

        return results;
    }

    private void runQueryMetadata(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);

            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(from);

            InvoiceMetadataResult response = context.client().invoices().queryMetadata(query);
            int count = response.invoices() != null ? response.invoices().size() : 0;
            boolean hasMore = response.hasMore();
            LOG.info("[{}] queryMetadata: {} invoices, hasMore={}", NAME, count, hasMore);
            results.add(RunResult.ok(NAME, OP_QUERY_METADATA, elapsed(start),
                    count + " invoices, hasMore=" + hasMore));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_METADATA, elapsed(start), errorMessage(exception)));
        }
    }

    private String runExportInvoices(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);

            java.security.PublicKey encKey = extractEncryptionKey(context);

            InvoiceExportBuilder exportBuilder = InvoiceExportBuilder.create(encKey)
                    .filters(InvoiceQueryBuilder.seller().invoicingDateFrom(from))
                    .metadataOnly();

            ExportInvoicesResult response = context.client().invoices().exportInvoices(exportBuilder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] export started, ref={}", NAME, refNum);
            context.state().setExportReferenceNumber(refNum);
            results.add(RunResult.ok(NAME, OP_EXPORT, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_EXPORT, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private static java.security.PublicKey extractEncryptionKey(DemoContext context) {
        PublicKeyCertificate cert = context.client().security().getPublicKeyCertificates().stream()
                .filter(c -> c.usage().contains(PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_ENCRYPTION_CERT));
        try {
            java.security.cert.CertificateFactory factory =
                    java.security.cert.CertificateFactory.getInstance(CERT_TYPE_X509);
            java.security.cert.X509Certificate x509 =
                    (java.security.cert.X509Certificate) factory.generateCertificate(
                            new java.io.ByteArrayInputStream(cert.certificate()));
            return x509.getPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException(ERR_KEY_EXTRACT, ex);
        }
    }

    private void pollExportStatus(DemoContext context, String exportRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        try {
            while (elapsed(start) < POLL_TIMEOUT_MS) {
                InvoiceExportStatus response = context.client().invoices()
                        .getExportStatus(exportRef);
                Integer code = response.status() != null ? response.status().code() : null;
                LOG.info("[{}] export status: code={}", NAME, code);
                if (code != null && code == EXPORT_STATUS_OK) {
                    results.add(RunResult.ok(NAME, OP_EXPORT_STATUS, elapsed(start),
                            "ready after " + elapsed(start) + "ms"));
                    return;
                }
                Thread.sleep(delay);
                delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, EXPORT_POLL_MAX_DELAY_MS);
            }
            results.add(RunResult.fail(NAME, OP_EXPORT_STATUS, elapsed(start),
                    "Timeout waiting for export status 200"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            results.add(RunResult.fail(NAME, OP_EXPORT_STATUS, elapsed(start), "Interrupted"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_EXPORT_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGetByKsefNumber(DemoContext context, String ksefNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            byte[] invoiceBytes = context.client().invoices().getByKsefNumber(ksefNumber);
            LOG.info("[{}] retrieved invoice by KSeF number, size={} bytes", NAME, invoiceBytes.length);
            results.add(RunResult.ok(NAME, OP_GET_BY_KSEF, elapsed(start),
                    invoiceBytes.length + " bytes"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_BY_KSEF, elapsed(start), errorMessage(exception)));
        }
    }
}
