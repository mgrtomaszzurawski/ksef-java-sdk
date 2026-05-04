/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incremental invoice export sync orchestrator.
 *
 * <p>Implements the documented HWM-based incremental retrieval algorithm
 * from {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}:
 *
 * <ol>
 *   <li>iterate per-subject-type independently;</li>
 *   <li>per window: open an export job ({@link InvoiceClient#prepareExport})
 *       with the cursor as {@code dateFrom};</li>
 *   <li>poll until terminal, download all parts, verify SHA-256, decrypt,
 *       unzip to {@code outputDirectory}/&lt;subject&gt;/window-&lt;n&gt;/;</li>
 *   <li>parse {@code _metadata.json} (array of invoice metadata) and
 *       dispatch each invoice through the {@link InvoiceSink};</li>
 *   <li>after all invoices in the package are accepted, persist the
 *       checkpoint via {@link InvoicePackage#continuationCursor()} —
 *       which honours {@code IsTruncated=true → LastPermanentStorageDate}
 *       per spec (REQ-HWM-002);</li>
 *   <li>repeat until the package count is zero or the cursor stops
 *       advancing.</li>
 * </ol>
 *
 * <p>Window adjacency (REQ-EXPORT-WINDOWING-001) is preserved by
 * always using the just-saved checkpoint as the next window's
 * {@code dateFrom} — no gaps between consecutive windows of the same
 * subject type.
 *
 * <p>De-duplication uses {@link KsefNumber}. Even if two consecutive
 * windows happen to overlap, the same KSeF number is dispatched at most
 * once.
 *
 * <p>Spec citations: REQ-HWM-001..003, REQ-EXPORT-WINDOWING-001/002.
 */
public final class InvoiceSyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceSyncClient.class);
    private static final int MAX_WINDOWS_PER_SUBJECT = 1_000;
    private static final String ERR_NULL_PLAN = "plan must not be null";
    private static final String ERR_NULL_STORE = "checkpointStore must not be null";
    private static final String ERR_NULL_SINK = "sink must not be null";

    private final InvoiceClient invoiceClient;
    private final ObjectMapper objectMapper;

    public InvoiceSyncClient(InvoiceClient invoiceClient, ObjectMapper objectMapper) {
        this.invoiceClient = Objects.requireNonNull(invoiceClient, "invoiceClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Run an incremental sync. See class-level Javadoc for the algorithm.
     *
     * @param plan sync configuration
     * @param checkpointStore where checkpoints are persisted between runs
     * @param sink invoice processor — called once per accepted invoice
     * @return per-subject-type counts and final checkpoints
     */
    public SyncResult sync(IncrementalSyncPlan plan, CheckpointStore checkpointStore, InvoiceSink sink) {
        Objects.requireNonNull(plan, ERR_NULL_PLAN);
        Objects.requireNonNull(checkpointStore, ERR_NULL_STORE);
        Objects.requireNonNull(sink, ERR_NULL_SINK);

        Map<InvoiceQuerySubjectType, Long> counts = new java.util.EnumMap<>(InvoiceQuerySubjectType.class);
        Map<InvoiceQuerySubjectType, SyncCheckpoint> finalCheckpoints =
                new java.util.EnumMap<>(InvoiceQuerySubjectType.class);

        for (InvoiceQuerySubjectType subjectType : plan.subjectTypes()) {
            long count = syncSubjectType(plan, subjectType, checkpointStore, sink);
            counts.put(subjectType, count);
            checkpointStore.load(subjectType).ifPresent(cp -> finalCheckpoints.put(subjectType, cp));
        }

        return new SyncResult(counts, finalCheckpoints);
    }

    private long syncSubjectType(IncrementalSyncPlan plan,
                                  InvoiceQuerySubjectType subjectType,
                                  CheckpointStore store,
                                  InvoiceSink sink) {
        OffsetDateTime cursor = store.load(subjectType)
                .map(SyncCheckpoint::cursor)
                .orElse(plan.from());

        Set<String> seenKsefNumbers = new HashSet<>();
        long processed = 0;

        for (int windowIndex = 0; windowIndex < MAX_WINDOWS_PER_SUBJECT; windowIndex++) {
            WindowOutcome outcome = processOneWindow(plan, subjectType, store, sink,
                    cursor, windowIndex, seenKsefNumbers);
            processed += outcome.processedDelta();
            if (outcome.advancedCursor() == null) {
                break;
            }
            cursor = outcome.advancedCursor();
        }

        return processed;
    }

    private WindowOutcome processOneWindow(IncrementalSyncPlan plan,
                                           InvoiceQuerySubjectType subjectType,
                                           CheckpointStore store,
                                           InvoiceSink sink,
                                           OffsetDateTime cursor,
                                           int windowIndex,
                                           Set<String> seenKsefNumbers) {
        InvoiceQueryBuilder query = subjectQuery(subjectType, plan.dateType(), cursor);
        if (plan.to() != null) {
            query.dateTo(plan.to());
        }

        try (PreparedInvoiceExport export = invoiceClient.prepareExport(query, plan.fullContent())) {
            InvoiceExportStatus status = export.awaitReady();
            InvoicePackage pkg = status.invoicePackage();
            if (pkg == null || pkg.invoiceCount() == null || pkg.invoiceCount() == 0L) {
                LOGGER.debug("[sync {} window {}] empty package — stop", subjectType, windowIndex);
                return WindowOutcome.stop(0L);
            }

            Path windowDir = plan.outputDirectory()
                    .resolve(subjectType.name().toLowerCase(Locale.ROOT))
                    .resolve("window-" + windowIndex);
            Files.createDirectories(windowDir);

            ExportedInvoiceDirectory dir = export.downloadAndDecryptTo(status, windowDir);
            List<InvoiceMetadata> metadatas = readMetadataJson(dir);
            long dispatched = dispatchInvoices(metadatas, dir, plan.fullContent(), sink, seenKsefNumbers, subjectType);

            // Commit-after-accept: only persist the checkpoint after every
            // invoice in this package was accepted by the sink. If the
            // sink threw, control already left this method and the
            // checkpoint stays at the previous value; the next run will
            // re-process this window.
            OffsetDateTime nextCursor = pkg.continuationCursor();
            boolean truncated = Boolean.TRUE.equals(pkg.isTruncated());
            if (nextCursor != null && (cursor == null || !nextCursor.equals(cursor))) {
                store.save(subjectType, new SyncCheckpoint(nextCursor, truncated));
                return WindowOutcome.advance(dispatched, nextCursor);
            }
            LOGGER.debug("[sync {} window {}] cursor did not advance — stop", subjectType, windowIndex);
            return WindowOutcome.stop(dispatched);
        } catch (IOException ex) {
            throw new KsefException("Failed to materialise sync window for " + subjectType, ex);
        }
    }

    private long dispatchInvoices(List<InvoiceMetadata> metadatas,
                                  ExportedInvoiceDirectory dir,
                                  boolean fullContent,
                                  InvoiceSink sink,
                                  Set<String> seenKsefNumbers,
                                  InvoiceQuerySubjectType subjectType) {
        long dispatched = 0;
        for (InvoiceMetadata metadata : metadatas) {
            KsefNumber typed = parsableUnseenKsefNumber(metadata, seenKsefNumbers, subjectType);
            if (typed == null) {
                continue;
            }
            Path xmlPath = fullContent ? matchInvoiceXml(dir, typed, (int) dispatched) : null;
            sink.accept(typed, metadata, xmlPath);
            seenKsefNumbers.add(metadata.ksefNumber());
            dispatched++;
        }
        return dispatched;
    }

    /**
     * Returns the parsed {@link KsefNumber} for {@code metadata}, or
     * {@code null} when the entry should be skipped (missing number,
     * already seen, or malformed). Logs a warning for malformed numbers
     * so the operator notices server-side data quality issues.
     */
    private KsefNumber parsableUnseenKsefNumber(InvoiceMetadata metadata,
                                                 Set<String> seenKsefNumbers,
                                                 InvoiceQuerySubjectType subjectType) {
        String raw = metadata.ksefNumber();
        if (raw == null || seenKsefNumbers.contains(raw)) {
            return null;
        }
        try {
            return KsefNumber.parse(raw);
        } catch (IllegalArgumentException badNumber) {
            LOGGER.warn("[sync {}] skipping invoice with invalid KSeF number: {}", subjectType, raw);
            return null;
        }
    }

    /**
     * Outcome of processing a single sync window. {@code advancedCursor}
     * is {@code null} when the loop should stop (empty package or
     * non-advancing cursor).
     */
    private record WindowOutcome(long processedDelta, OffsetDateTime advancedCursor) {
        static WindowOutcome advance(long delta, OffsetDateTime cursor) {
            return new WindowOutcome(delta, cursor);
        }

        static WindowOutcome stop(long delta) {
            return new WindowOutcome(delta, null);
        }
    }

    private List<InvoiceMetadata> readMetadataJson(ExportedInvoiceDirectory dir) {
        Path metadataPath = dir.metadataJson();
        if (metadataPath == null) {
            return List.of();
        }
        try {
            byte[] bytes = Files.readAllBytes(metadataPath);
            // _metadata.json per spec is a JSON array of InvoiceMetadata-shaped
            // objects (the wire shape returned by /invoices/query/metadata,
            // minus the pagination envelope). Parse as InvoiceMetadataRaw[]
            // then map via the same mappers used by InvoiceClient.
            InvoiceMetadataRaw[] rawArray = objectMapper.readValue(bytes, InvoiceMetadataRaw[].class);
            return java.util.Arrays.stream(rawArray)
                    .map(InvoicingMappers::toInvoiceMetadata)
                    .toList();
        } catch (IOException ex) {
            throw new KsefException("Failed to parse _metadata.json from export package", ex);
        }
    }

    /**
     * Best-effort match of a metadata entry to an on-disk XML file.
     *
     * <p>The KSeF spec does not strictly mandate file naming inside the
     * export ZIP; common patterns observed include {@code {ksefNumber}.xml},
     * {@code faktura_{n}.xml}, and {@code invoice-{n}.xml}. We try each.
     * Falls back to the n-th .xml file in stable lexical order if no name
     * matches. Returns {@code null} if no XML files are present.
     */
    private static Path matchInvoiceXml(ExportedInvoiceDirectory dir, KsefNumber typed, int ordinal) {
        Map<String, Path> all = dir.invoiceXmls();
        Path byKsefNumber = all.get(typed.value() + ".xml");
        if (byKsefNumber != null) {
            return byKsefNumber;
        }
        Path byFaktura = all.get("faktura_" + (ordinal + 1) + ".xml");
        if (byFaktura != null) {
            return byFaktura;
        }
        Path byInvoice = all.get("invoice-" + (ordinal + 1) + ".xml");
        if (byInvoice != null) {
            return byInvoice;
        }
        java.util.List<String> sorted = new java.util.ArrayList<>(all.keySet());
        Collections.sort(sorted);
        if (ordinal >= 0 && ordinal < sorted.size()) {
            return all.get(sorted.get(ordinal));
        }
        return null;
    }

    private static InvoiceQueryBuilder subjectQuery(InvoiceQuerySubjectType type,
                                                     InvoiceQueryDateType dateType,
                                                     OffsetDateTime cursor) {
        InvoiceQueryBuilder builder = switch (type) {
            case SUBJECT1 -> InvoiceQueryBuilder.seller();
            case SUBJECT2 -> InvoiceQueryBuilder.buyer();
            case SUBJECT3 -> InvoiceQueryBuilder.thirdParty();
            case SUBJECT_AUTHORIZED -> InvoiceQueryBuilder.authorized();
        };
        // The builder implicitly assigns dateType from which "From" setter
        // we call (PERMANENT_STORAGE / INVOICING / ISSUE).
        return switch (dateType) {
            case PERMANENT_STORAGE -> builder.permanentStorageDateFrom(cursor);
            case INVOICING -> builder.invoicingDateFrom(cursor);
            case ISSUE -> builder.issueDateFrom(cursor);
        };
    }
}
