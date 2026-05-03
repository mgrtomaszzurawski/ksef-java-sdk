/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incremental invoice sync orchestrator.
 *
 * <p>Implements the documented HWM-based pagination algorithm from
 * {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md}:
 * iterate per subject type, advance the cursor via
 * {@code permanentStorageHwmDate} on each page, dedupe by KSeF number,
 * persist a checkpoint after each batch of invoices is accepted.
 *
 * <p>Acquired via {@code KsefClient.invoiceSync()} (Tier 1 workflow API
 * per ADR-021).
 *
 * <p>Spec citations: REQ-HWM-001..003, REQ-EXPORT-WINDOWING-002.
 */
public final class InvoiceSyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceSyncClient.class);
    private static final int DEFAULT_PAGE_FETCH_LIMIT = 1_000_000;
    private static final String ERR_NULL_PLAN = "plan must not be null";
    private static final String ERR_NULL_STORE = "checkpointStore must not be null";
    private static final String ERR_NULL_SINK = "sink must not be null";

    private final InvoiceClient invoiceClient;

    public InvoiceSyncClient(InvoiceClient invoiceClient) {
        this.invoiceClient = Objects.requireNonNull(invoiceClient, "invoiceClient must not be null");
    }

    /**
     * Run an incremental sync.
     *
     * @param plan the sync plan (date range, subject types, date type)
     * @param checkpointStore where checkpoints are persisted
     * @param sink invoice processor — called once per accepted invoice
     * @return per-subject-type counts and final checkpoints
     */
    public SyncResult sync(IncrementalSyncPlan plan, CheckpointStore checkpointStore, InvoiceSink sink) {
        Objects.requireNonNull(plan, ERR_NULL_PLAN);
        Objects.requireNonNull(checkpointStore, ERR_NULL_STORE);
        Objects.requireNonNull(sink, ERR_NULL_SINK);

        Map<InvoiceQuerySubjectType, Long> counts = new HashMap<>();
        Map<InvoiceQuerySubjectType, SyncCheckpoint> finalCheckpoints = new HashMap<>();

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

        while (true) {
            InvoiceQueryBuilder query = subjectQueryBuilder(subjectType, plan.dateType())
                    .permanentStorageDateFrom(cursor);
            if (plan.to() != null) {
                query.dateTo(plan.to());
            }

            InvoiceMetadataResult page = invoiceClient.queryMetadata(query);
            int pageInvoices = 0;
            for (InvoiceMetadata metadata : page.invoices()) {
                if (metadata.ksefNumber() == null) {
                    continue;
                }
                if (seenKsefNumbers.contains(metadata.ksefNumber())) {
                    continue;
                }
                KsefNumber typed;
                try {
                    typed = KsefNumber.parse(metadata.ksefNumber());
                } catch (IllegalArgumentException badNumber) {
                    LOGGER.warn("Skipping invoice with invalid KSeF number: {}", metadata.ksefNumber());
                    continue;
                }
                sink.accept(typed, metadata);
                seenKsefNumbers.add(metadata.ksefNumber());
                processed++;
                pageInvoices++;
            }

            // Persist checkpoint AFTER all invoices in this page were accepted.
            // Commit-after-accept guarantee per CheckpointStore javadoc.
            OffsetDateTime nextCursor = page.permanentStorageHwmDate();
            if (nextCursor != null) {
                store.save(subjectType,
                        new SyncCheckpoint(nextCursor, page.isTruncated()));
                cursor = nextCursor;
            }

            if (!page.hasMore() || page.permanentStorageHwmDate() == null || pageInvoices == 0) {
                break;
            }
            if (processed >= DEFAULT_PAGE_FETCH_LIMIT) {
                LOGGER.warn("Sync hit DEFAULT_PAGE_FETCH_LIMIT={} for {}; resume on next call",
                        DEFAULT_PAGE_FETCH_LIMIT, subjectType);
                break;
            }
        }

        return processed;
    }

    private static InvoiceQueryBuilder subjectQueryBuilder(InvoiceQuerySubjectType type,
                                                            InvoiceQueryDateType dateType) {
        InvoiceQueryBuilder builder = switch (type) {
            case SUBJECT1 -> InvoiceQueryBuilder.seller();
            case SUBJECT2 -> InvoiceQueryBuilder.buyer();
            case SUBJECT3 -> InvoiceQueryBuilder.thirdParty();
            case SUBJECT_AUTHORIZED -> InvoiceQueryBuilder.authorized();
        };
        // Date type configuration on the builder is a no-op for now since the
        // default is PERMANENT_STORAGE which matches our sync recommendation.
        // Once InvoiceQueryBuilder exposes a public dateType setter, wire here.
        return builder;
    }
}
