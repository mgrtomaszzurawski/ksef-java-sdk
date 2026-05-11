/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Workflow-level tests for {@link PreparedInvoiceExport}: poll-until-ready,
 * download/decrypt, file-backed download, ZIP-safety guards, and lifecycle
 * zeroization on close.
 *
 * <p>WireMock serves package-part bytes on a relative URL the SDK then GETs
 * directly via the bundled {@link HttpClient}. The {@link InvoiceExport}
 * dependency is mocked to drive {@code awaitReady()} state transitions
 * without round-tripping a real session.
 *
 * <p>Covers TC-INV-009, TC-INV-010, TC-INV-011, TC-INV-012, TC-INV-013, TC-INV-014.
 */
@WireMockTest(httpsEnabled = true)
class PreparedInvoiceExportWorkflowTest {

    private static final String DISABLE_HOSTNAME_VERIFY_PROPERTY =
            "jdk.internal.httpclient.disableHostnameVerification";
    private static String previousDisableHostnameVerify;

    @org.junit.jupiter.api.BeforeAll
    static void disableHostnameVerification() {
        // Capture the previous value so AfterAll can restore the JVM exactly as it was.
        // WireMock serves a self-signed cert whose CN/SAN does not match "localhost";
        // the JDK HttpClient honours this single property for SAN-mismatch suppression.
        // Test scope only — the SDK's own HttpClient validates certs as normal.
        previousDisableHostnameVerify = System.getProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY);
        System.setProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY, "true");
    }

    @org.junit.jupiter.api.AfterAll
    static void restoreHostnameVerification() {
        if (previousDisableHostnameVerify == null) {
            System.clearProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY);
        } else {
            System.setProperty(DISABLE_HOSTNAME_VERIFY_PROPERTY, previousDisableHostnameVerify);
        }
    }

    private static final String EXPORT_REF = "20260418-EX-1111111111-ABCDEF1234-01";
    private static final String PART_PATH = "/parts/part1.bin";
    private static final String METADATA_FILE = "_metadata.json";
    private static final String INVOICE_NAME = "faktura_1.xml";
    private static final byte[] METADATA_CONTENT = "[]".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVOICE_CONTENT = "<Invoice/>".getBytes(StandardCharsets.UTF_8);
    private static final int STATUS_OK = 200;
    private static final int STATUS_PROCESSING = 100;
    private static final int STATUS_TERMINAL_FAILURE = 415;

    @Test
    void close_zeroizesAesKeyAndInitVector(WireMockRuntimeInfo wmInfo) throws Exception {
        // given
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        try (PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey.clone(), initVector.clone())) {

            // when
            handle.close();

            // then
            byte[] storedKey = readByteField(handle, "aesKey");
            byte[] storedIv = readByteField(handle, "initVector");
            for (byte byteValue : storedKey) {
                assertEquals((byte) 0, byteValue, "AES key must be zeroized after close()");
            }
            for (byte byteValue : storedIv) {
                assertEquals((byte) 0, byteValue, "IV must be zeroized after close()");
            }
        }
    }

    @Test
    void close_isIdempotent(WireMockRuntimeInfo wmInfo) {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());

        // when — first and second close on same handle
        handle.close();

        // then — second close is a no-op (no exception, no state corruption)
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(handle::close);
    }

    @Test
    void downloadAndDecrypt_afterClose_throwsIllegalState() {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());
        InvoiceExportStatus dummy = okStatus(List.of());
        handle.close();

        // when / then
        assertThrows(IllegalStateException.class, () -> handle.downloadAndDecrypt(dummy));
    }

    @Test
    void awaitReady_whenStatus200_returnsTerminalStatus() {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        InvoiceExportStatus terminal = okStatus(List.of());
        when(invoiceExport.getStatus(anyString())).thenReturn(terminal);
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());

        // when
        InvoiceExportStatus result = handle.awaitReady();

        // then
        assertEquals(STATUS_OK, result.status().code());
    }

    @Test
    void awaitReady_whenTerminalFailure_throwsTerminalFailureException() {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        InvoiceExportStatus failure = new InvoiceExportStatus(
                new StatusInfo(STATUS_TERMINAL_FAILURE, "Schema invalid", List.of()),
                null, null, null);
        when(invoiceExport.getStatus(anyString())).thenReturn(failure);
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());

        // when / then
        KsefSessionTerminalFailureException ex = assertThrows(KsefSessionTerminalFailureException.class,
                handle::awaitReady);
        assertEquals(STATUS_TERMINAL_FAILURE, ex.code());
    }

    @Test
    void downloadAndDecrypt_whenNonHttpsUrl_throwsKsefException() {
        // given — http:// instead of https://
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create("http://example.com/x.bin"), null, null, null, null, null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());

        // when / then
        KsefException ex = assertThrows(KsefException.class, () -> handle.downloadAndDecrypt(status));
        assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("non-https")
                        || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("https"),
                "Error message must mention HTTPS requirement; got: " + ex.getMessage());
    }

    @Test
    void downloadAndDecrypt_whenUnsupportedMethod_throwsKsefException() {
        // given — POST instead of GET
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "POST",
                URI.create("https://example.com/x.bin"), null, null, null, null, null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, CryptoService.generateAesKey(), CryptoService.generateIv());

        // when / then
        KsefException ex = assertThrows(KsefException.class, () -> handle.downloadAndDecrypt(status));
        assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("method"),
                "Error must mention method; got: " + ex.getMessage());
    }

    @Test
    void downloadAndDecrypt_whenHashMismatch_throwsKsefException(WireMockRuntimeInfo wmInfo) {
        // given — server returns bytes whose SHA-256 does NOT match the
        // declared encryptedPartHash. The SDK must reject before even attempting decrypt.
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        byte[] zipBytes = buildZip();
        byte[] encryptedZip = CryptoService.encryptAes(zipBytes, aesKey, iv);

        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse()
                        .withStatus(STATUS_OK)
                        .withBody(encryptedZip)));

        // bogus hash so verification fails
        byte[] wrongHash = new byte[32];
        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                (long) zipBytes.length, sha256(zipBytes),
                (long) encryptedZip.length, wrongHash, null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey, iv);

        // when / then
        KsefException ex = assertThrows(KsefException.class, () -> handle.downloadAndDecrypt(status));
        assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("sha-256")
                        || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("hash")
                        || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("mismatch"),
                "Error must mention hash mismatch; got: " + ex.getMessage());
    }

    @Test
    void downloadAndDecrypt_happyPath_returnsParsedPackage(WireMockRuntimeInfo wmInfo) {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        byte[] zipBytes = buildZip();
        byte[] encryptedZip = CryptoService.encryptAes(zipBytes, aesKey, iv);

        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse()
                        .withStatus(STATUS_OK)
                        .withBody(encryptedZip)));

        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                (long) zipBytes.length, sha256(zipBytes),
                (long) encryptedZip.length, sha256(encryptedZip), null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey, iv);

        // when
        ExportedInvoicePackage result = handle.downloadAndDecrypt(status);

        // then
        assertNotNull(result);
        assertNotNull(result.metadataJson());
        assertEquals("[]", new String(result.metadataJson(), StandardCharsets.UTF_8));
        assertTrue(result.invoiceXmls().containsKey(INVOICE_NAME),
                "Expected invoice in unzipped package; got: " + result.invoiceXmls().keySet());
    }

    @Test
    void downloadAndDecryptTo_writesEntriesToDirectory(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        byte[] zipBytes = buildZip();
        byte[] encryptedZip = CryptoService.encryptAes(zipBytes, aesKey, iv);

        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse().withStatus(STATUS_OK).withBody(encryptedZip)));

        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                (long) zipBytes.length, sha256(zipBytes),
                (long) encryptedZip.length, sha256(encryptedZip), null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey, iv);

        Path outputDir = tempDir.resolve("export");

        // when
        ExportedInvoiceDirectory result = handle.downloadAndDecryptTo(status, outputDir);

        // then
        assertNotNull(result.metadataJson(), "metadata.json path expected");
        assertTrue(Files.exists(result.metadataJson()),
                "metadata.json file must be created");
        assertTrue(result.invoiceXmls().containsKey(INVOICE_NAME),
                "Invoice file must be tracked in the result");
        // Temp parts directory must be cleaned up
        assertFalse(Files.exists(outputDir.resolve(".parts")),
                ".parts directory must be removed after successful decrypt");
    }

    @Test
    void downloadAndDecrypt_whenZipSlipEntryName_throwsKsefException(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir) {
        // given — ZIP entry attempts to write outside the output directory.
        // We craft a ZIP whose entry name is "../escape.txt".
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();
        byte[] zipBytes = buildMaliciousZipSlip();
        byte[] encryptedZip = CryptoService.encryptAes(zipBytes, aesKey, iv);

        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse().withStatus(STATUS_OK).withBody(encryptedZip)));

        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                (long) zipBytes.length, sha256(zipBytes),
                (long) encryptedZip.length, sha256(encryptedZip), null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey, iv);

        Path outputDir = tempDir.resolve("export");

        // when / then
        KsefException ex = assertThrows(KsefException.class,
                () -> handle.downloadAndDecryptTo(status, outputDir));
        assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("escapes"),
                "Error must mention zip-slip / escape; got: " + ex.getMessage());
    }

    @Test
    void downloadAndDecrypt_whenServerReturns404_throwsKsefException(WireMockRuntimeInfo wmInfo) {
        // given
        InvoiceExport invoiceExport = mock(InvoiceExport.class);
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] iv = CryptoService.generateIv();

        stubFor(get(urlEqualTo(PART_PATH))
                .willReturn(aResponse().withStatus(404)));

        InvoicePackagePart part = new InvoicePackagePart(1, "part1.bin", "GET",
                URI.create(wmInfo.getHttpsBaseUrl() + PART_PATH),
                null, null, null, null, null);
        InvoiceExportStatus status = okStatus(List.of(part));
        PreparedInvoiceExport handle = newHandle(invoiceExport, aesKey, iv);

        // when / then
        KsefException ex = assertThrows(KsefException.class, () -> handle.downloadAndDecrypt(status));
        assertTrue(ex.getMessage().contains("404") || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("download"),
                "Error must mention status or download failure; got: " + ex.getMessage());
    }

    private static InvoiceExportStatus okStatus(List<InvoicePackagePart> parts) {
        InvoicePackage pkg = new InvoicePackage(
                (long) parts.size(), null, parts, null, null, null, null, null);
        return new InvoiceExportStatus(new StatusInfo(STATUS_OK, "OK", List.of()),
                null, null, pkg);
    }

    private static byte[] buildZip() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(METADATA_FILE));
            zip.write(METADATA_CONTENT);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(INVOICE_NAME));
            zip.write(INVOICE_CONTENT);
            zip.closeEntry();
        } catch (java.io.IOException ex) {
            throw new AssertionError("ZIP construction failed", ex);
        }
        return buffer.toByteArray();
    }

    private static byte[] buildMaliciousZipSlip() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("../escape.txt"));
            zip.write("escape".getBytes(StandardCharsets.UTF_8));
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

    private static PreparedInvoiceExport newHandle(InvoiceExport invoiceExport, byte[] aesKey, byte[] iv) {
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionHandleConstructor.newPreparedExport(invoiceExport, insecureHttpClient(), EXPORT_REF, aesKey, iv);
    }

    /**
     * HttpClient that trusts WireMock's self-signed certificate AND skips
     * hostname verification (WireMock's cert subject is not "localhost").
     * Test scope only — the SDK's own HttpClient validates certs as normal.
     */
    private static HttpClient insecureHttpClient() {
        // Hostname-verification suppression is set/restored at @BeforeAll/@AfterAll
        // so it does not contaminate other tests in the same JVM (Codex F9).
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

    private static byte[] readByteField(PreparedInvoiceExport handle, String name) throws Exception {
        Field field = PreparedInvoiceExport.class.getDeclaredField(name);
        field.setAccessible(true);
        return (byte[]) field.get(handle);
    }
}
