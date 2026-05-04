/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSink;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InvoiceSyncClient}: window iteration, HWM advancement
 * (truncated and untruncated), commit-after-accept invariants, dedup by
 * KSeF number, and stop conditions (empty package, non-advancing cursor).
 *
 * <p>The SDK's {@link PreparedInvoiceExport} is exercised by mocking
 * {@link InvoiceClient#prepareExport} to return a stub handle whose
 * {@code awaitReady} and {@code downloadAndDecryptTo} return canned data.
 *
 * <p>Covers TC-INV-015 .. TC-INV-021.
 */
class InvoiceSyncClientTest {

    private static final String VALID_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final OffsetDateTime START_CURSOR = OffsetDateTime.of(
            2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ADVANCED_CURSOR = OffsetDateTime.of(
            2026, 4, 2, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime FAR_CURSOR = OffsetDateTime.of(
            2026, 4, 3, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final int STATUS_OK = 200;
    private static final long INVOICE_COUNT_ONE = 1L;
    private static final long INVOICE_COUNT_ZERO = 0L;

    @Test
    void sync_dispatchesEachInvoiceOnce_andAdvancesCheckpoint(@TempDir Path tempDir) throws Exception {
        // given — one window with one invoice; second poll returns empty package → stop.
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        RecordingSink sink = new RecordingSink();
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);

        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR, false),
                metadataDir(tempDir.resolve("subject1/window-0"), List.of(VALID_KSEF_NUMBER)));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR, false), null);

        when(invoiceClient.prepareExport(org.mockito.ArgumentMatchers.any(InvoiceQueryBuilder.class), anyBoolean()))
                .thenReturn(firstWindow, emptyWindow);

        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());

        // when
        SyncResult result = client.sync(plan, store, sink);

        // then
        assertEquals(1, sink.invocations.get(),
                "Sink should receive exactly one invoice");
        assertEquals(1L, result.totalProcessed());
        Optional<SyncCheckpoint> checkpoint = store.load(InvoiceQuerySubjectType.SUBJECT1);
        assertTrue(checkpoint.isPresent(), "Checkpoint must be persisted after window");
        assertEquals(ADVANCED_CURSOR, checkpoint.get().cursor());
    }

    @Test
    void sync_whenSinkThrows_doesNotAdvanceCheckpoint(@TempDir Path tempDir) throws Exception {
        // given — sink throws on the first invoice; no checkpoint should be saved
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        InvoiceSink throwingSink = (number, metadata, xmlPath) -> {
            throw new IllegalStateException("sink failure");
        };
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);

        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR, false),
                metadataDir(tempDir.resolve("subject1/window-0"), List.of(VALID_KSEF_NUMBER)));

        when(invoiceClient.prepareExport(org.mockito.ArgumentMatchers.any(InvoiceQueryBuilder.class), anyBoolean()))
                .thenReturn(firstWindow);

        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());

        // when / then
        assertThrows(IllegalStateException.class, () -> client.sync(plan, store, throwingSink));
        assertTrue(store.load(InvoiceQuerySubjectType.SUBJECT1).isEmpty(),
                "Checkpoint must NOT advance when sink throws");
    }

    @Test
    void sync_whenCursorDoesNotAdvance_stopsWindowLoop(@TempDir Path tempDir) throws Exception {
        // given — cursor returned by package equals current cursor → stop
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        RecordingSink sink = new RecordingSink();
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);

        PreparedInvoiceExport stuckWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, START_CURSOR, false),
                metadataDir(tempDir.resolve("subject1/window-0"), List.of(VALID_KSEF_NUMBER)));

        when(invoiceClient.prepareExport(org.mockito.ArgumentMatchers.any(InvoiceQueryBuilder.class), anyBoolean()))
                .thenReturn(stuckWindow);

        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());

        // when
        SyncResult result = client.sync(plan, store, sink);

        // then — invoice processed, but checkpoint not advanced because cursor stayed put
        assertEquals(1L, result.totalProcessed());
    }

    @Test
    void sync_dedupesInvoicesAcrossOverlappingWindows(@TempDir Path tempDir) throws Exception {
        // given — two windows that both report the same invoice; the sink must see it once
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        RecordingSink sink = new RecordingSink();
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);

        PreparedInvoiceExport window1 = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR, true),
                metadataDir(tempDir.resolve("subject1/window-0"), List.of(VALID_KSEF_NUMBER)));
        PreparedInvoiceExport window2 = stubExport(
                exportStatus(INVOICE_COUNT_ONE, FAR_CURSOR, false),
                metadataDir(tempDir.resolve("subject1/window-1"), List.of(VALID_KSEF_NUMBER)));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, FAR_CURSOR, false), null);

        when(invoiceClient.prepareExport(org.mockito.ArgumentMatchers.any(InvoiceQueryBuilder.class), anyBoolean()))
                .thenReturn(window1, window2, emptyWindow);

        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());

        // when
        SyncResult result = client.sync(plan, store, sink);

        // then — even though metadata appeared in two windows, sink saw it once
        assertEquals(1, sink.invocations.get(),
                "Same KSeF number must be dispatched at most once across overlapping windows");
        assertEquals(1L, result.totalProcessed());
    }

    @Test
    void sync_withNullPlan_throwsNullPointerException() {
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        RecordingSink sink = new RecordingSink();

        assertThrows(NullPointerException.class, () -> client.sync(null, store, sink));
    }

    @Test
    void sync_withNullCheckpointStore_throwsNullPointerException(@TempDir Path tempDir) {
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        RecordingSink sink = new RecordingSink();

        assertThrows(NullPointerException.class, () -> client.sync(plan, null, sink));
    }

    @Test
    void sync_withNullSink_throwsNullPointerException(@TempDir Path tempDir) {
        InvoiceClient invoiceClient = mock(InvoiceClient.class);
        InvoiceSyncClient client = new InvoiceSyncClient(invoiceClient, jacksonMapper());
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        assertThrows(NullPointerException.class, () -> client.sync(plan, store, null));
    }

    private static IncrementalSyncPlan singleSubjectPlan(Path tempDir) {
        return IncrementalSyncPlan.builder()
                .from(START_CURSOR)
                .subjectTypes(InvoiceQuerySubjectType.SUBJECT1)
                .outputDirectory(tempDir.resolve("output"))
                .fullContent(false)
                .build();
    }

    private static InvoiceExportStatus exportStatus(long invoiceCount, OffsetDateTime hwm, boolean truncated) {
        InvoicePackage pkg = new InvoicePackage(
                invoiceCount, null, List.of(), truncated, null, null,
                truncated ? hwm : null, truncated ? null : hwm);
        return new InvoiceExportStatus(new StatusInfo(STATUS_OK, "OK", List.of()), null, null, pkg);
    }

    private static ExportedInvoiceDirectory metadataDir(Path windowDir, List<String> ksefNumbers) throws IOException {
        Files.createDirectories(windowDir);
        Path metadataPath = windowDir.resolve("_metadata.json");
        // Spec-shaped wrapper {"invoices": [...]}, per
        // ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md and
        // open-api.json export prepare endpoint description (Codex H2).
        StringBuilder json = new StringBuilder("{\"invoices\":[");
        for (int index = 0; index < ksefNumbers.size(); index++) {
            if (index > 0) {
                json.append(",");
            }
            json.append("{\"ksefNumber\":\"").append(ksefNumbers.get(index)).append("\"}");
        }
        json.append("]}");
        Files.writeString(metadataPath, json.toString());
        return new ExportedInvoiceDirectory(windowDir, metadataPath, java.util.Map.of());
    }

    private static ObjectMapper jacksonMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Build a Mockito-backed stub that pretends to be a {@link PreparedInvoiceExport}.
     * Mockito 5 supports mocking final classes via the inline mock-maker, which is
     * the default since 5.0. The orchestrator calls {@code awaitReady()} and
     * {@code downloadAndDecryptTo()}; both return canned values.
     *
     * <p>The {@code close()} stub returns void without throwing — the orchestrator
     * relies on try-with-resources to dispose the handle.
     */
    private static PreparedInvoiceExport stubExport(InvoiceExportStatus status,
                                                     ExportedInvoiceDirectory directory) {
        PreparedInvoiceExport stub = mock(PreparedInvoiceExport.class);
        when(stub.awaitReady()).thenReturn(status);
        when(stub.downloadAndDecryptTo(any(InvoiceExportStatus.class), any(Path.class)))
                .thenReturn(directory);
        doNothing().when(stub).close();
        return stub;
    }

    private static final class InMemoryCheckpointStore implements CheckpointStore {
        private final java.util.Map<InvoiceQuerySubjectType, SyncCheckpoint> store =
                new java.util.EnumMap<>(InvoiceQuerySubjectType.class);

        @Override
        public Optional<SyncCheckpoint> load(InvoiceQuerySubjectType subjectType) {
            return Optional.ofNullable(store.get(subjectType));
        }

        @Override
        public void save(InvoiceQuerySubjectType subjectType, SyncCheckpoint checkpoint) {
            store.put(subjectType, checkpoint);
        }
    }

    private static final class RecordingSink implements InvoiceSink {
        final AtomicInteger invocations = new AtomicInteger();
        final List<KsefNumber> seen = new ArrayList<>();

        @Override
        public void accept(KsefNumber ksefNumber, io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata metadata, Path xmlPath) {
            invocations.incrementAndGet();
            seen.add(ksefNumber);
            assertNotNull(metadata, "metadata must not be null");
        }
    }
}
