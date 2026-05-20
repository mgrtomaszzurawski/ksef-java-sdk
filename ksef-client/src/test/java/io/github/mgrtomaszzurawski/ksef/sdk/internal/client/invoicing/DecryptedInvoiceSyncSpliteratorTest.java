/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.UnrecognizedInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DecryptedInvoiceSyncSpliterator}: producer-thread happy path,
 * cooperative cancel via {@link DecryptedInvoiceSyncSpliterator#close()}, and
 * producer-failure propagation through {@code tryAdvance}.
 *
 * <p>Producer thread runs the real {@link io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.sync.InvoiceSyncClient}
 * over a Mockito-stubbed {@link Invoices#prepareExport}, just like
 * {@code InvoiceSyncClientTest} does. The Spliterator's queueing sink is
 * exercised indirectly through that path.
 */
class DecryptedInvoiceSyncSpliteratorTest {

    private static final String KSEF_NUMBER_1 = "5265877635-20250826-0100001AF629-AF";
    private static final OffsetDateTime START_CURSOR = OffsetDateTime.of(
            2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ADVANCED_CURSOR = OffsetDateTime.of(
            2026, 4, 2, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final int STATUS_OK = 200;
    private static final long INVOICE_COUNT_ZERO = 0L;
    private static final long INVOICE_COUNT_ONE = 1L;
    private static final String CUSTOM_FORM_SYSTEM_CODE = "KIT-X";
    private static final String CUSTOM_FORM_SCHEMA_VERSION = "1-0";
    private static final String CUSTOM_FORM_VALUE = "KitX";

    @Test
    void tryAdvance_whenProducerEmitsInvoice_drainsItThenReturnsFalse(@TempDir Path tempDir) throws Exception {
        // given — one window with one invoice, then empty window to stop iteration
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        Path windowDir = tempDir.resolve("subject1/window-0");
        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR),
                metadataDirWithXml(windowDir, List.of(KSEF_NUMBER_1)));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(firstWindow, emptyWindow);

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        // when — collect the stream
        AtomicInteger emitted = new AtomicInteger();
        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store)) {
            while (spliterator.tryAdvance(invoice -> emitted.incrementAndGet())) {
                // drain
            }
        }

        // then — invoice observed; checkpoint persisted
        assertEquals(1, emitted.get(), "Spliterator must emit the invoice exactly once");
        Optional<SyncCheckpoint> checkpoint = store.load(InvoiceQuerySubjectType.SUBJECT1);
        assertTrue(checkpoint.isPresent(), "Checkpoint must be persisted after window");
    }

    @Test
    void close_whenCalled_stopsFurtherTryAdvance(@TempDir Path tempDir) throws Exception {
        // given — single emitter window, consumer closes after one invoice
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        Path windowDir = tempDir.resolve("subject1/window-0");
        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR),
                metadataDirWithXml(windowDir, List.of(KSEF_NUMBER_1)));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(firstWindow, emptyWindow);

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store);

        // when — drain one, close, attempt another
        AtomicInteger emitted = new AtomicInteger();
        boolean firstAdvanced = spliterator.tryAdvance(invoice -> emitted.incrementAndGet());
        spliterator.close();
        boolean secondAdvanced = spliterator.tryAdvance(invoice -> emitted.incrementAndGet());

        // then — first tryAdvance succeeded; second returns false post-close
        assertTrue(firstAdvanced, "first tryAdvance must succeed");
        assertFalse(secondAdvanced, "tryAdvance after close must return false");
        assertEquals(1, emitted.get(), "exactly one invoice emitted before close");
    }

    @Test
    void tryAdvance_whenProducerThrowsIllegalState_rethrowsAsIs(@TempDir Path tempDir) {
        // given — Invoices.prepareExport throws IllegalStateException →
        // producer thread fails with that exact type. propagateProducerFailure
        // rethrows RuntimeException subclasses as-is (without wrapping in
        // KsefException), so the test pins that branch by asserting the
        // exact thrown type.
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenThrow(new IllegalStateException("simulated upstream failure"));

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store)) {
            assertThrows(IllegalStateException.class,
                    () -> drainAll(spliterator),
                    "IllegalStateException from producer must propagate as-is, not wrapped");
        }
    }

    private static void drainAll(DecryptedInvoiceSyncSpliterator spliterator) {
        while (spliterator.tryAdvance(invoice -> { /* no-op */ })) {
            // drain until producer completes or fails
        }
    }

    @Test
    void tryAdvance_whenActionIsNull_throwsNullPointerException(@TempDir Path tempDir) {
        // given — minimal sync setup (empty window)
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(emptyWindow);

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        // when / then
        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store)) {
            assertThrows(NullPointerException.class, () -> spliterator.tryAdvance(null));
        }
    }

    @Test
    void tryAdvance_whenRegisteredCustomFormCode_emitsTypedCustomDocument(@TempDir Path tempDir) throws Exception {
        // given — metadata carries a custom form code triple; spliterator built
        // with a registry mapping that triple to a typed wrapper class.
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        Path windowDir = tempDir.resolve("subject1/window-0");
        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR),
                metadataDirWithCustomFormCode(windowDir, KSEF_NUMBER_1,
                        CUSTOM_FORM_SYSTEM_CODE, CUSTOM_FORM_SCHEMA_VERSION, CUSTOM_FORM_VALUE));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(firstWindow, emptyWindow);

        KsefInvoiceTypes registry = KsefInvoiceTypes.builder()
                .register(SampleCustomInvoiceDocument.class)
                .build();
        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        // when — collect emitted document
        java.util.concurrent.atomic.AtomicReference<InvoiceDocument> emitted =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store, registry)) {
            while (spliterator.tryAdvance(invoice -> emitted.compareAndSet(null, invoice.document()))) {
                // drain
            }
        }

        // then — emitted document is the typed custom wrapper, not the fallback
        InvoiceDocument document = emitted.get();
        assertNotNull(document, "Spliterator must emit one document");
        assertInstanceOf(SampleCustomInvoiceDocument.class, document,
                "Registered custom form code must surface as typed wrapper, not UnrecognizedInvoiceDocument");
    }

    @Test
    void tryAdvance_whenUnregisteredCustomFormCode_emitsUnrecognizedDocument(@TempDir Path tempDir) throws Exception {
        // given — metadata carries a custom form code that the registry does NOT cover
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        Path windowDir = tempDir.resolve("subject1/window-0");
        PreparedInvoiceExport firstWindow = stubExport(
                exportStatus(INVOICE_COUNT_ONE, ADVANCED_CURSOR),
                metadataDirWithCustomFormCode(windowDir, KSEF_NUMBER_1,
                        CUSTOM_FORM_SYSTEM_CODE, CUSTOM_FORM_SCHEMA_VERSION, CUSTOM_FORM_VALUE));
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(firstWindow, emptyWindow);

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        // when — registry without the custom registration → fallback path
        java.util.concurrent.atomic.AtomicReference<InvoiceDocument> emitted =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store, KsefInvoiceTypes.builtinsOnly())) {
            while (spliterator.tryAdvance(invoice -> emitted.compareAndSet(null, invoice.document()))) {
                // drain
            }
        }

        // then — UnrecognizedInvoiceDocument carrying the reported form code
        InvoiceDocument document = emitted.get();
        assertNotNull(document);
        assertInstanceOf(UnrecognizedInvoiceDocument.class, document);
        assertEquals(FormCode.custom(CUSTOM_FORM_SYSTEM_CODE,
                        CUSTOM_FORM_SCHEMA_VERSION, CUSTOM_FORM_VALUE),
                document.formCode());
    }

    @Test
    void characteristics_isOrderedAndNonnull(@TempDir Path tempDir) {
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        PreparedInvoiceExport emptyWindow = stubExport(
                exportStatus(INVOICE_COUNT_ZERO, ADVANCED_CURSOR), null);
        when(invoiceExport.prepare(any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenReturn(emptyWindow);

        IncrementalSyncPlan plan = singleSubjectPlan(tempDir);
        InMemoryCheckpointStore store = new InMemoryCheckpointStore();

        try (DecryptedInvoiceSyncSpliterator spliterator = new DecryptedInvoiceSyncSpliterator(
                invoiceExport, jacksonMapper(), plan, store)) {
            int characteristics = spliterator.characteristics();
            assertNotEquals(0, characteristics & java.util.Spliterator.ORDERED, "ORDERED bit set");
            assertNotEquals(0, characteristics & java.util.Spliterator.NONNULL, "NONNULL bit set");
        }
    }

    // ---------- helpers ----------

    private static IncrementalSyncPlan singleSubjectPlan(Path tempDir) {
        return IncrementalSyncPlan.builder()
                .from(START_CURSOR)
                .subjectTypes(InvoiceQuerySubjectType.SUBJECT1)
                .outputDirectory(tempDir.resolve("output"))
                .fullContent(false)
                .build();
    }

    private static InvoiceExportStatus exportStatus(long invoiceCount, OffsetDateTime hwm) {
        InvoicePackage pkg = new InvoicePackage(
                invoiceCount, null, List.of(), false, null, null, null, hwm);
        return new InvoiceExportStatus(new StatusInfo(STATUS_OK, "OK", List.of()), null, null, pkg);
    }

    private static ExportedInvoiceDirectory metadataDirWithCustomFormCode(Path windowDir,
                                                                           String ksefNumber,
                                                                           String systemCode,
                                                                           String schemaVersion,
                                                                           String formValue) throws IOException {
        Files.createDirectories(windowDir);
        Path metadataPath = windowDir.resolve("_metadata.json");
        String json = String.format(
                "{\"invoices\":[{\"ksefNumber\":\"%s\",\"formCode\":{\"systemCode\":\"%s\","
                        + "\"schemaVersion\":\"%s\",\"value\":\"%s\"}}]}",
                ksefNumber, systemCode, schemaVersion, formValue);
        Files.writeString(metadataPath, json);
        Path xmlPath = windowDir.resolve(ksefNumber + ".xml");
        Files.writeString(xmlPath, "<KitX/>");
        return new ExportedInvoiceDirectory(windowDir, metadataPath,
                Map.of(ksefNumber + ".xml", xmlPath));
    }

    private static ExportedInvoiceDirectory metadataDirWithXml(Path windowDir, List<String> ksefNumbers) throws IOException {
        Files.createDirectories(windowDir);
        Path metadataPath = windowDir.resolve("_metadata.json");
        StringBuilder json = new StringBuilder("{\"invoices\":[");
        for (int index = 0; index < ksefNumbers.size(); index++) {
            if (index > 0) {
                json.append(",");
            }
            json.append("{\"ksefNumber\":\"").append(ksefNumbers.get(index)).append("\"}");
        }
        json.append("]}");
        Files.writeString(metadataPath, json.toString());
        java.util.Map<String, Path> xmlFiles = new java.util.HashMap<>();
        for (String ksefNumber : ksefNumbers) {
            Path xmlPath = windowDir.resolve(ksefNumber + ".xml");
            Files.writeString(xmlPath, "<Invoice/>");
            xmlFiles.put(ksefNumber + ".xml", xmlPath);
        }
        return new ExportedInvoiceDirectory(windowDir, metadataPath, Map.copyOf(xmlFiles));
    }

    private static PreparedInvoiceExport stubExport(InvoiceExportStatus status,
                                                     ExportedInvoiceDirectory directory) {
        PreparedInvoiceExport stub = mock(PreparedInvoiceExport.class);
        when(stub.awaitReady()).thenReturn(status);
        when(stub.downloadAndDecryptTo(any(InvoiceExportStatus.class), any(Path.class)))
                .thenReturn(directory);
        doNothing().when(stub).close();
        return stub;
    }

    private static ObjectMapper jacksonMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Fixture custom InvoiceDocument matching the FormCode triple emitted by the test metadata. */
    public static final class SampleCustomInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = FormCode.custom(
                CUSTOM_FORM_SYSTEM_CODE, CUSTOM_FORM_SCHEMA_VERSION, CUSTOM_FORM_VALUE);
        private final byte[] xml;
        private SampleCustomInvoiceDocument(byte[] xml) {
            this.xml = xml.clone();
        }
        public static SampleCustomInvoiceDocument from(byte[] xml) {
            return new SampleCustomInvoiceDocument(xml);
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return xml.clone();
        }
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
}
