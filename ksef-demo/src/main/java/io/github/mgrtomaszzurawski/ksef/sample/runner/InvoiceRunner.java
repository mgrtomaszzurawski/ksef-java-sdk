/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_BACKOFF_MULTIPLIER;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_INITIAL_DELAY_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_TIMEOUT_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for InvoiceClient operations.
 *
 * <p>All operations run in both AUTH_SAFE and FULL modes — they're either
 * read-only (queryInvoicesByMetadata, getExportStatus, getByKsefNumber) or start a
 * non-destructive read-side job (exportInvoices). In AUTH_SAFE mode
 * getByKsefNumber falls back to the first invoice from queryInvoicesByMetadata
 * when the FULL-mode SessionRunner did not populate a KSeF number.</p>
 */
public final class InvoiceRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceRunner.class);
    private static final String NAME = "invoice";
    private static final String OP_QUERY_METADATA = "queryInvoicesByMetadata";
    private static final String OP_EXPORT = "exportInvoices";
    private static final String OP_EXPORT_STATUS = "getExportStatus";
    private static final String OP_GET_BY_KSEF = "getByKsefNumber";
    private static final String SKIP_NO_KSEF_NUMBER =
            "no KSeF number available — queryInvoicesByMetadata returned empty and no FULL-mode ref in context";
    private static final String SKIP_NO_EXPORT_REF = "export not started";
    private static final int EXPORT_STATUS_OK = 200;
    private static final int EXPORT_POLL_MAX_DELAY_MS = 10000;
    private static final int QUERY_DATE_RANGE_DAYS = 30;
    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        InvoiceMetadataResult metadata = runQueryMetadata(context, results);

        String exportRef = runExportInvoices(context, results);
        if (exportRef != null) {
            pollExportStatus(context, exportRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_EXPORT_STATUS, SKIP_NO_EXPORT_REF));
        }

        // Prefer the KSeF number captured by SessionRunner in FULL mode; fall back to the first
        // metadata result so AUTH_SAFE can exercise getByKsefNumber against an existing invoice.
        String ksefNumber = context.invoiceKsefNumber();
        if (ksefNumber == null && metadata != null && metadata.invoices() != null && !metadata.invoices().isEmpty()) {
            ksefNumber = metadata.invoices().get(0).ksefNumber();
        }
        if (ksefNumber != null) {
            runGetByKsefNumber(context, ksefNumber, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF, SKIP_NO_KSEF_NUMBER));
        }

        return results;
    }

    private InvoiceMetadataResult runQueryMetadata(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);

            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(from);

            InvoiceMetadataResult response = context.client().invoices().queryInvoicesByMetadata(query.build());
            int count = response.invoices() != null ? response.invoices().size() : 0;
            boolean hasMore = response.hasMore();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] queryInvoicesByMetadata: {} invoices, hasMore={}", NAME, count, hasMore);
            }
            results.add(RunResult.ok(NAME, OP_QUERY_METADATA, elapsed(start),
                    count + " invoices, hasMore=" + hasMore));
            return response;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_METADATA, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runExportInvoices(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);

            // prepareExport handles symmetric-key fetch, AES-key generation, and
            // package-decrypt material retention; demo only needs the reference
            // number to drive status polling. fullContent=false → metadata only.
            try (PreparedInvoiceExport export = context.client().invoices().prepareExport(
                    InvoiceQueryBuilder.seller().invoicingDateFrom(from).build(), false)) {
                String refNum = export.referenceNumber();
                LOGGER.info("[{}] export started, ref={}", NAME, refNum);
                context.state().setExportReferenceNumber(refNum);
                results.add(RunResult.ok(NAME, OP_EXPORT, elapsed(start), "ref=" + refNum));
                return refNum;
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_EXPORT, elapsed(start), errorMessage(exception)));
            return null;
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
                LOGGER.info("[{}] export status: code={}", NAME, code);
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
            byte[] invoiceBytes = context.client().invoices().getByKsefNumber(KsefNumber.parse(ksefNumber));
            LOGGER.info("[{}] retrieved invoice by KSeF number, size={} bytes", NAME, invoiceBytes.length);
            results.add(RunResult.ok(NAME, OP_GET_BY_KSEF, elapsed(start),
                    invoiceBytes.length + " bytes"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_BY_KSEF, elapsed(start), errorMessage(exception)));
        }
    }
}
