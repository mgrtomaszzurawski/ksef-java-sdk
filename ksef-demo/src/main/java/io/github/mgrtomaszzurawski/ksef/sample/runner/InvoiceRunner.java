/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_BACKOFF_MULTIPLIER;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_INITIAL_DELAY_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_TIMEOUT_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for Invoices operations.
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
    private static final String OP_GET_BY_KSEF_TYPED = "getByKsefNumberTyped";
    private static final String OP_SYNC_AS_STREAM = "syncAsStream";
    private static final String OP_EXPORT_DOWNLOAD = "exportDownload";
    private static final String EXPORT_DOWNLOAD_PREFIX = "ksef-demo-export-";
    private static final String SKIP_NO_KSEF_NUMBER =
            "no KSeF number available — queryInvoicesByMetadata returned empty and no FULL-mode ref in context";
    private static final String SKIP_NO_EXPORT_REF = "export not started";
    private static final String SKIP_NO_KSEF_NUMBER_TYPED =
            "no KSeF number available — typed-doc probe needs an existing KSeF number";
    private static final String SKIP_NOT_FULL_MODE = "FULL-only — typed-doc probe needs a known KSeF number";
    private static final int EXPORT_STATUS_OK = 200;
    private static final int EXPORT_POLL_MAX_DELAY_MS = 10000;
    private static final int QUERY_DATE_RANGE_DAYS = 30;
    private static final int SYNC_WINDOW_DAYS = 7;
    private static final long SYNC_STREAM_LIMIT = 5L;
    private static final String SYNC_OUTPUT_PREFIX = "ksef-demo-sync-";
    private static final String LABEL_FA3 = "FA3";
    private static final String LABEL_FA2 = "FA2";
    private static final String LABEL_PEF = "PEF";
    private static final String LABEL_PEF_KOR = "PEF_KOR";
    private static final String LABEL_RAW_FALLBACK = "raw-fallback";
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
            ksefNumber = metadata.invoices().get(0).ksefNumber().value();
        }
        if (ksefNumber != null) {
            runGetByKsefNumber(context, ksefNumber, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF, SKIP_NO_KSEF_NUMBER));
        }

        runSyncAsStream(context, results);
        runGetByKsefNumberTyped(context, ksefNumber, results);
        runExportDownload(context, results);

        return results;
    }

    /**
     * End-to-end export download: open a fresh {@code PreparedInvoiceExport}
     * (METADATA_ONLY scope keeps download small), wait for terminal status,
     * call {@link PreparedInvoiceExport#downloadAndDecryptTo} to actually
     * retrieve and decrypt the archive bytes, and assert the result. Pins
     * the terminal-state retrieval path that the original
     * {@code runExportInvoices} + {@code pollExportStatus} pair only
     * exercised up to the status-200 observation.
     */
    private void runExportDownload(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        java.nio.file.Path outputDir = null;
        try {
            // Files.createTempDirectory honours the JVM's umask on Unix
            // (POSIX dir created mode 0700 by default); the deleteRecursive
            // finally block scrubs the dir after the probe. Demo code,
            // not production SDK surface.
            @SuppressWarnings("java:S5443")
            java.nio.file.Path tmp = Files.createTempDirectory(EXPORT_DOWNLOAD_PREFIX);
            outputDir = tmp;
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);
            try (PreparedInvoiceExport export = context.client().invoices().export().prepare(
                    InvoiceQueryBuilder.seller().invoicingDateFrom(from).build(),
                    ExportScope.METADATA_ONLY)) {
                InvoiceExportStatus status = export.awaitReady();
                Integer code = status.status() != null ? status.status().code() : null;
                if (code == null || code != EXPORT_STATUS_OK) {
                    results.add(RunResult.fail(NAME, OP_EXPORT_DOWNLOAD, elapsed(start),
                            "non-terminal status code=" + code));
                    return;
                }
                var directory = export.downloadAndDecryptTo(status, outputDir);
                int xmlCount = directory.invoiceXmls() != null ? directory.invoiceXmls().size() : 0;
                results.add(RunResult.ok(NAME, OP_EXPORT_DOWNLOAD, elapsed(start),
                        "downloaded " + xmlCount + " invoice XMLs to temp dir"));
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_EXPORT_DOWNLOAD, elapsed(start),
                    errorMessage(exception)));
        } finally {
            deleteRecursive(outputDir);
        }
    }

    private static void deleteRecursive(java.nio.file.Path dir) {
        if (dir == null) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException ignored) {
                            // best-effort cleanup
                        }
                    });
        } catch (java.io.IOException ignored) {
            // best-effort cleanup
        }
    }

    private InvoiceMetadataResult runQueryMetadata(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(QUERY_DATE_RANGE_DAYS);

            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(from);

            InvoiceMetadataResult response = context.client().invoices().archive().queryByMetadata(query.build());
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
            try (PreparedInvoiceExport export = context.client().invoices().export().prepare(
                    InvoiceQueryBuilder.seller().invoicingDateFrom(from).build(), ExportScope.METADATA_ONLY)) {
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
                InvoiceExportStatus response = context.client().invoices().export().getStatus(exportRef);
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
            InvoiceDocument invoiceDocument = context.client().invoices().archive().getByKsefNumber(KsefNumber.parse(ksefNumber));
            int xmlLength = invoiceDocument.xml().length;
            LOGGER.info("[{}] retrieved invoice by KSeF number, size={} bytes", NAME, xmlLength);
            results.add(RunResult.ok(NAME, OP_GET_BY_KSEF, elapsed(start),
                    xmlLength + " bytes"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_BY_KSEF, elapsed(start), errorMessage(exception)));
        }
    }

    /**
     * Smoke-test the {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices#syncAsStream}
     * stream entry point (PR16): walk up to {@value #SYNC_STREAM_LIMIT}
     * decrypted invoices in a {@value #SYNC_WINDOW_DAYS}-day window
     * ending now, persisting per-window checkpoints into an in-memory
     * store. The probe passes whether or not the KSeF env actually has
     * invoices in the window — empty windows are documented and OK.
     */
    private void runSyncAsStream(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        Path outputDirectory = null;
        try {
            outputDirectory = Files.createTempDirectory(SYNC_OUTPUT_PREFIX);
            OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .minusDays(SYNC_WINDOW_DAYS);
            IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                    .from(from)
                    .outputDirectory(outputDirectory)
                    .fullContent(true)
                    .build();
            CheckpointStore store = CheckpointStore.inMemory();
            long count;
            try (Stream<DecryptedInvoice> stream =
                         context.client().invoices().sync().asStream(plan, store)) {
                count = stream.limit(SYNC_STREAM_LIMIT).count();
            }
            LOGGER.info("[{}] syncAsStream walked {} invoices (limit {})", NAME, count, SYNC_STREAM_LIMIT);
            String message = count == 0
                    ? "0 invoices in window"
                    : count + " invoices walked";
            results.add(RunResult.ok(NAME, OP_SYNC_AS_STREAM, elapsed(start), message));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SYNC_AS_STREAM, elapsed(start),
                    errorMessage(exception)));
        } finally {
            cleanupSyncOutput(outputDirectory);
        }
    }

    private static void cleanupSyncOutput(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best-effort cleanup; leftover sync output is harmless
                        }
                    });
        } catch (IOException ignored) {
            // best-effort cleanup; leftover sync output is harmless
        }
    }

    /**
     * Exercise the typed-doc branch of {@code getByKsefNumber}: the
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices#getByKsefNumber}
     * overload returning {@link InvoiceDocument} (PR12b). Only runs in
     * FULL mode when a real KSeF number is captured by an earlier probe.
     */
    private void runGetByKsefNumberTyped(DemoContext context, String ksefNumber, List<RunResult> results) {
        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF_TYPED, SKIP_NOT_FULL_MODE));
            return;
        }
        if (ksefNumber == null) {
            results.add(RunResult.skip(NAME, OP_GET_BY_KSEF_TYPED, SKIP_NO_KSEF_NUMBER_TYPED));
            return;
        }
        long start = System.currentTimeMillis();
        try {
            InvoiceDocument invoiceDocument = context.client().invoices().archive().getByKsefNumber(KsefNumber.parse(ksefNumber));
            String typeName = labelFor(invoiceDocument);
            LOGGER.info("[{}] typed invoice document: {}", NAME, typeName);
            results.add(RunResult.ok(NAME, OP_GET_BY_KSEF_TYPED, elapsed(start), typeName));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_BY_KSEF_TYPED, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private static String labelFor(InvoiceDocument invoiceDocument) {
        if (invoiceDocument instanceof Fa3InvoiceDocument) {
            return LABEL_FA3;
        }
        if (invoiceDocument instanceof Fa2InvoiceDocument) {
            return LABEL_FA2;
        }
        if (invoiceDocument instanceof PefKorInvoiceDocument) {
            return LABEL_PEF_KOR;
        }
        if (invoiceDocument instanceof PefInvoiceDocument) {
            return LABEL_PEF;
        }
        return LABEL_RAW_FALLBACK;
    }
}
