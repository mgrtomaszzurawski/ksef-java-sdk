/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint;
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
import org.jspecify.annotations.Nullable;
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
 *   <li>per window: open an export job ({@link InvoiceExport#prepare})
 *       with the cursor as {@code dateFrom};</li>
 *   <li>poll until terminal, download all parts, verify SHA-256, decrypt,
 *       unzip to {@code outputDirectory}/&lt;subject&gt;/window-&lt;n&gt;/;</li>
 *   <li>parse {@code _metadata.json} as the spec-shaped object wrapper
 *       {@code { "invoices": [...] }} (the bare-array shape produced by
 *       earlier draft documentation is also tolerated for compatibility)
 *       and dispatch each invoice through the {@link InvoiceSink};</li>
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
 *
 * @since 1.0.0
 */
public final class InvoiceSyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceSyncClient.class);
    private static final int MAX_WINDOWS_PER_SUBJECT = 1_000;
    /** Standard KSeF export ZIP entries are XML files. */
    private static final String XML_EXTENSION = ".xml";
    /** Common SDK / KSeF ZIP naming convention #1 — {@code faktura_N.xml}. */
    private static final String FAKTURA_NAME_PREFIX = "faktura_";
    /** Common SDK / KSeF ZIP naming convention #2 — {@code invoice-N.xml}. */
    private static final String INVOICE_NAME_PREFIX = "invoice-";
    /** Top-level field name in {@code _metadata.json} per KSeF spec (object wrapper). */
    private static final String METADATA_INVOICES_FIELD = "invoices";
    private static final String ERR_NULL_PLAN = "plan must not be null";
    private static final String ERR_NULL_STORE = "checkpointStore must not be null";
    private static final String ERR_NULL_SINK = "sink must not be null";
    private static final String ERR_METADATA_PARSE = "Failed to parse _metadata.json from export package";
    private static final String ERR_METADATA_SHAPE = "_metadata.json must be either an array or an object with an 'invoices' array";
    private static final String ERR_METADATA_MISSING = "Export package reports invoiceCount=%d but the decrypted package has no _metadata.json — refusing to advance the checkpoint (Codex round-9 fresh-review F3 fail-closed)";
    private static final String ERR_METADATA_EMPTY = "Export package reports invoiceCount=%d but _metadata.json is empty — refusing to advance the checkpoint (Codex round-9 fresh-review F3 fail-closed)";

    private final InvoiceExport invoiceExport;
    private final ObjectMapper objectMapper;

    public InvoiceSyncClient(InvoiceExport invoiceExport, ObjectMapper objectMapper) {
        this.invoiceExport = Objects.requireNonNull(invoiceExport, "invoiceExport must not be null");
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

        SubjectSyncContext context = new SubjectSyncContext(plan, subjectType, store, sink);
        Set<String> seenKsefNumbers = new HashSet<>();
        long processed = 0;

        for (int windowIndex = 0; windowIndex < MAX_WINDOWS_PER_SUBJECT; windowIndex++) {
            WindowOutcome outcome = processOneWindow(context, cursor, windowIndex, seenKsefNumbers);
            processed += outcome.processedDelta();
            if (outcome.advancedCursor() == null) {
                break;
            }
            cursor = outcome.advancedCursor();
        }

        return processed;
    }

    /**
     * Per-subject-type sync context — bundles the immutable parameters
     * shared across all windows of one subject type so the per-window
     * methods stay narrow. The seenKsefNumbers de-duplication accumulator
     * is intentionally NOT a record component because it mutates across
     * windows; passing it as an explicit parameter keeps the record's
     * value-semantics promise honest.
     */
    private record SubjectSyncContext(IncrementalSyncPlan plan,
                                      InvoiceQuerySubjectType subjectType,
                                      CheckpointStore store,
                                      InvoiceSink sink) { }

    private WindowOutcome processOneWindow(SubjectSyncContext context,
                                           OffsetDateTime cursor,
                                           int windowIndex,
                                           Set<String> seenKsefNumbers) {
        IncrementalSyncPlan plan = context.plan();
        InvoiceQuerySubjectType subjectType = context.subjectType();
        InvoiceQueryBuilder query = subjectQuery(subjectType, IncrementalSyncPlan.DATE_TYPE, cursor);
        if (plan.to() != null) {
            query.dateTo(plan.to());
        }

        try (PreparedInvoiceExport export = invoiceExport.prepare(query.build(), plan.fullContent() ? ExportScope.FULL_CONTENT : ExportScope.METADATA_ONLY)) {
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
            // Codex round-9 fresh-review F3 — fail closed if KSeF reports a
            // non-empty package but the decrypted directory is missing the
            // metadata file (or it's empty). Otherwise the dispatch loop
            // emits zero invoices and the checkpoint advances anyway,
            // permanently skipping this window.
            long reportedCount = pkg.invoiceCount();
            if (dir.metadataJson() == null) {
                throw new KsefException(String.format(Locale.ROOT, ERR_METADATA_MISSING, reportedCount), null);
            }
            if (metadatas.isEmpty()) {
                throw new KsefException(String.format(Locale.ROOT, ERR_METADATA_EMPTY, reportedCount), null);
            }
            long dispatched = dispatchInvoices(context, metadatas, dir, seenKsefNumbers);

            // Commit-after-accept: only persist the checkpoint after every
            // invoice in this package was accepted by the sink. If the
            // sink threw, control already left this method and the
            // checkpoint stays at the previous value; the next run will
            // re-process this window.
            OffsetDateTime nextCursor = pkg.continuationCursor();
            boolean truncated = Boolean.TRUE.equals(pkg.isTruncated());
            if (nextCursor != null && (cursor == null || !nextCursor.equals(cursor))) {
                context.store().save(subjectType, new SyncCheckpoint(nextCursor, truncated));
                return WindowOutcome.advance(dispatched, nextCursor);
            }
            LOGGER.debug("[sync {} window {}] cursor did not advance — stop", subjectType, windowIndex);
            return WindowOutcome.stop(dispatched);
        } catch (IOException ex) {
            throw new KsefException("Failed to materialise sync window for " + subjectType, ex);
        }
    }

    private long dispatchInvoices(SubjectSyncContext context,
                                  List<InvoiceMetadata> metadatas,
                                  ExportedInvoiceDirectory dir,
                                  Set<String> seenKsefNumbers) {
        long dispatched = 0;
        for (InvoiceMetadata metadata : metadatas) {
            KsefNumber typed = parsableUnseenKsefNumber(metadata, seenKsefNumbers, context.subjectType());
            if (typed == null) {
                continue;
            }
            Path xmlPath = context.plan().fullContent() ? matchInvoiceXml(dir, typed, (int) dispatched) : null;
            context.sink().accept(typed, metadata, xmlPath);
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
    private @Nullable KsefNumber parsableUnseenKsefNumber(InvoiceMetadata metadata,
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
    private record WindowOutcome(long processedDelta, @Nullable OffsetDateTime advancedCursor) {
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
            // Codex H2: KSeF spec (export prepare endpoint description in
            // open-api.json + ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md
            // section "Plik _metadata.json") states the file is an object
            // with an "invoices" array, e.g. {"invoices": [...]}. Earlier
            // drafts of this SDK assumed a bare array — keep tolerating
            // that shape so existing on-disk packages still parse.
            JsonNode root = objectMapper.readTree(bytes);
            JsonNode invoicesNode;
            if (root.isArray()) {
                invoicesNode = root;
            } else if (root.isObject() && root.has(METADATA_INVOICES_FIELD)
                    && root.get(METADATA_INVOICES_FIELD).isArray()) {
                invoicesNode = root.get(METADATA_INVOICES_FIELD);
            } else {
                throw new KsefException(ERR_METADATA_SHAPE, null);
            }
            List<InvoiceMetadata> out = new java.util.ArrayList<>(invoicesNode.size());
            for (JsonNode element : invoicesNode) {
                InvoiceMetadataRaw raw = objectMapper.treeToValue(element, InvoiceMetadataRaw.class);
                out.add(InvoicingMappers.toInvoiceMetadata(raw));
            }
            return List.copyOf(out);
        } catch (IOException ex) {
            throw new KsefException(ERR_METADATA_PARSE, ex);
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
    private static @Nullable Path matchInvoiceXml(ExportedInvoiceDirectory dir, KsefNumber typed, int ordinal) {
        Map<String, Path> all = dir.invoiceXmls();
        Path byKsefNumber = all.get(typed.value() + XML_EXTENSION);
        if (byKsefNumber != null) {
            return byKsefNumber;
        }
        Path byFaktura = all.get(FAKTURA_NAME_PREFIX + (ordinal + 1) + XML_EXTENSION);
        if (byFaktura != null) {
            return byFaktura;
        }
        Path byInvoice = all.get(INVOICE_NAME_PREFIX + (ordinal + 1) + XML_EXTENSION);
        if (byInvoice != null) {
            return byInvoice;
        }
        List<String> sorted = new java.util.ArrayList<>(all.keySet());
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
        InvoiceQueryBuilder withDate = switch (dateType) {
            case PERMANENT_STORAGE -> builder.permanentStorageDateFrom(cursor);
            case INVOICING -> builder.invoicingDateFrom(cursor);
            case ISSUE -> builder.issueDateFrom(cursor);
        };
        // Codex round-9 manual-validation A.1.1 — incremental-sync workflow
        // mandates restrictToPermanentStorageHwmDate=true on every export
        // (przyrostowe-pobieranie-faktur.md "Kluczowe znaczenie daty
        // PermanentStorage"). The flag caps dateRange.to at the server-side
        // HWM so consecutive windows never cross an unstable edge.
        return withDate.restrictToPermanentStorageHwm();
    }
}
