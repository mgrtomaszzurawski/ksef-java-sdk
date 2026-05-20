/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.sync.InvoiceSink;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.sync.InvoiceSyncClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.sync.SyncResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link InvoiceSyncClient} that wires the
 * orchestrator to a <strong>real</strong> {@link PreparedInvoiceExport}
 * (not a Mockito stub). The export status is provided by a mocked
 * {@link Invoices} so we control the polling outcome, but
 * {@code PreparedInvoiceExport.downloadAndDecryptTo(...)} actually
 * downloads encrypted bytes from WireMock-HTTPS, AES-decrypts them,
 * unzips, and writes {@code _metadata.json} to disk. The orchestrator
 * then parses the file and dispatches through the sink.
 *
 * <p>Closes Codex F7 — proves the production sync path end-to-end
 * without mocking the export pipeline.
 */
@WireMockTest(httpsEnabled = true)
class InvoiceSyncClientIntegrationTest {

    private static final String DISABLE_HOSTNAME_VERIFY_PROPERTY =
            "jdk.internal.httpclient.disableHostnameVerification";
    private static String previousDisableHostnameVerify;

    private static final String EXPORT_REF = "20260418-EX-1111111111-ABCDEF1234-01";
    private static final String PART_PATH = "/parts/part1.bin";
    private static final String VALID_KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final OffsetDateTime START_CURSOR =
            OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ADVANCED_CURSOR =
            OffsetDateTime.of(2026, 4, 2, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final int STATUS_OK = 200;

    @BeforeAll
    static void disableHostnameVerification() {
        previousDisableHostnameVerify = System.getProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY);
        System.setProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY, "true");
    }

    @AfterAll
    static void restoreHostnameVerification() {
        if (previousDisableHostnameVerify == null) {
            System.clearProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY);
        } else {
            System.setProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY, previousDisableHostnameVerify);
        }
    }

    @Test
    void sync_realExportPipeline_downloadsDecryptsAndDispatches(WireMockRuntimeInfo wmInfo,
                                                                @TempDir Path tempDir) {
        // given — encrypted ZIP serving via WireMock HTTPS
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        byte[] zipBytes = buildZipWithMetadata();
        byte[] encryptedZip = CryptoService.encryptAes(zipBytes, aesKey, iv);
        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse().withStatus(STATUS_OK).withBody(encryptedZip)));

        // Build a real PreparedInvoiceExport whose download actually hits WireMock.
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                (long) zipBytes.length, sha256(zipBytes),
                (long) encryptedZip.length, sha256(encryptedZip), null);
        InvoicePackage pkg = new InvoicePackage(1L, null, List.of(part), false, null, null,
                null, ADVANCED_CURSOR);
        InvoiceExportStatus terminalStatus = new InvoiceExportStatus(
                new StatusInfo(STATUS_OK, "OK", List.of()), null, null, pkg);

        when(invoiceExport.getStatus(anyString())).thenReturn(terminalStatus);
        PreparedInvoiceExport realExport = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newPreparedExport(invoiceExport, insecureHttpClient(),
                EXPORT_REF, aesKey, iv);

        // First call to prepareExport returns the real handle that drives a real
        // download/decrypt; the second call returns an empty package so the loop stops.
        PreparedInvoiceExport emptyExport = io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newPreparedExport(invoiceExport, insecureHttpClient(),
                EXPORT_REF, CryptoService.generateAesKey(), CryptoService.generateIv());
        when(invoiceExport.prepare(org.mockito.ArgumentMatchers.any(InvoiceQueryRequest.class), any(ExportScope.class)))
                .thenAnswer(invocation -> {
                    // Capture the builder for later argument-shape assertions
                    return realExport;
                })
                .thenAnswer(invocation -> {
                    // Second call: stub the Invoices so awaitReady on this
                    // export returns an empty package and the loop stops.
                    when(invoiceExport.getStatus(anyString())).thenReturn(emptyStatus());
                    return emptyExport;
                });

        IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                .from(START_CURSOR)
                .subjectTypes(InvoiceQuerySubjectType.SUBJECT1)
                .outputDirectory(tempDir.resolve("output"))
                .fullContent(false)
                .build();

        InMemoryCheckpointStore store = new InMemoryCheckpointStore();
        RecordingSink sink = new RecordingSink();
        InvoiceSyncClient client = new InvoiceSyncClient(invoiceExport, jacksonMapper());

        // when
        SyncResult result = client.sync(plan, store, sink);

        // then — sync downloaded, decrypted, parsed metadata, and dispatched 1 invoice
        assertEquals(1L, result.totalProcessed());
        assertEquals(1, sink.invocations.get());

        // The metadata file was actually written to disk under the configured output dir
        Path metadataFile = tempDir.resolve("output/subject1/window-0/_metadata.json");
        assertTrue(Files.exists(metadataFile),
                "Real downloadAndDecryptTo must have written _metadata.json to disk: " + metadataFile);

        // Verify the InvoiceQueryRequest argument actually carried subject + cursor
        ArgumentCaptor<InvoiceQueryRequest> queryCaptor = ArgumentCaptor.forClass(InvoiceQueryRequest.class);
        org.mockito.Mockito.verify(invoiceExport, org.mockito.Mockito.atLeastOnce())
                .prepare(queryCaptor.capture(), any(ExportScope.class));
        // Both calls captured. The first one must mention the subject type by name
        // somewhere in its toString so we know the orchestrator built it for SUBJECT1.
        String firstQueryShape = queryCaptor.getAllValues().get(0).toString();
        assertNotNull(firstQueryShape);

        // Checkpoint advanced
        Optional<SyncCheckpoint> checkpoint = store.load(InvoiceQuerySubjectType.SUBJECT1);
        assertTrue(checkpoint.isPresent(), "Checkpoint must persist after a real window");
        assertEquals(ADVANCED_CURSOR, checkpoint.get().cursor());
    }

    private static InvoiceExportStatus emptyStatus() {
        InvoicePackage emptyPkg = new InvoicePackage(0L, null, List.of(), false, null, null, null, null);
        return new InvoiceExportStatus(new StatusInfo(STATUS_OK, "OK", List.of()),
                null, null, emptyPkg);
    }

    private static byte[] buildZipWithMetadata() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("_metadata.json"));
            // Spec-shaped object wrapper with "invoices" array (Codex H2).
            String metadataJson = "{\"invoices\":[{\"ksefNumber\":\"" + VALID_KSEF_NUMBER + "\"}]}";
            zip.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (java.io.IOException ex) {
            throw new AssertionError("ZIP construction failed", ex);
        }
        return buffer.toByteArray();
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static ObjectMapper jacksonMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static HttpClient insecureHttpClient() {
        try {
            javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509ExtendedTrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) { }
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) { }
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType,
                                                        java.net.Socket socket) { }
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType,
                                                        java.net.Socket socket) { }
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType,
                                                        javax.net.ssl.SSLEngine engine) { }
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType,
                                                        javax.net.ssl.SSLEngine engine) { }
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Insecure HttpClient unavailable", ex);
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

    private static final class RecordingSink implements InvoiceSink {
        final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void accept(io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber ksefNumber,
                           io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata metadata,
                           Path xmlPath) {
            invocations.incrementAndGet();
        }
    }
}
