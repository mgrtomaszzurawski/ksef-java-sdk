/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsefVerificationLinksTest {

    private static final String SELLER_NIP = "1111111111";
    private static final QrContextType CONTEXT_TYPE = QrContextType.NIP;
    private static final String CONTEXT_TYPE_WIRE = QrContextType.NIP.wireValue();
    private static final String CONTEXT_VALUE = "0987654321";
    private static final String CERTIFICATE_SERIAL = "00112233";
    private static final byte[] SIGNATURE = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 4, 4);
    private static final String SAMPLE_INVOICE_XML = "<Faktura>fixture invoice xml content</Faktura>";

    @Test
    void buildInvoiceVerificationUrl_whenTestEnvironment_returnsTestQrHostWithFormattedDate()
            throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);

        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                QrEnvironment.TEST, SELLER_NIP, ISSUE_DATE, hash);

        assertTrue(url.startsWith("https://qr-test.ksef.mf.gov.pl/invoice/"),
                "expected qr-test host, got: " + url);
        assertTrue(url.contains("/" + SELLER_NIP + "/"),
                "expected sellerNip in path, got: " + url);
        assertTrue(url.contains("/04-04-2026/"),
                "expected DD-MM-YYYY date format, got: " + url);
    }

    @Test
    void buildInvoiceVerificationUrl_whenProdEnvironment_returnsProdQrHost()
            throws NoSuchAlgorithmException {
        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                QrEnvironment.PROD, SELLER_NIP, ISSUE_DATE, sha256(SAMPLE_INVOICE_XML));

        assertTrue(url.startsWith("https://qr.ksef.mf.gov.pl/invoice/"),
                "expected qr.ksef.mf.gov.pl host, got: " + url);
    }

    @Test
    void buildInvoiceVerificationUrl_whenDemoEnvironment_returnsDemoQrHost()
            throws NoSuchAlgorithmException {
        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                QrEnvironment.DEMO, SELLER_NIP, ISSUE_DATE, sha256(SAMPLE_INVOICE_XML));

        assertTrue(url.startsWith("https://qr-demo.ksef.mf.gov.pl/invoice/"),
                "expected qr-demo host, got: " + url);
    }

    @Test
    void buildInvoiceVerificationUrl_whenInvoiceHashEncoded_usesBase64UrlNoPadding()
            throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);

        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                QrEnvironment.TEST, SELLER_NIP, ISSUE_DATE, hash);

        // Base64 URL alphabet uses only [A-Za-z0-9_-]; padding '=' must not appear.
        String hashSegment = url.substring(url.lastIndexOf('/') + 1);
        assertTrue(hashSegment.matches("[A-Za-z0-9_-]+"),
                "hash segment must use base64url alphabet, got: " + hashSegment);
        assertFalse(hashSegment.contains("="),
                "hash segment must not contain padding, got: " + hashSegment);
    }

    @Test
    void buildCertificateVerificationUrl_whenAllFieldsProvided_buildsCertificatePath()
            throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);
        var params = new KsefVerificationLinks.CertificateVerificationParams(
                CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL, hash, SIGNATURE);

        String url = KsefVerificationLinks.buildCertificateVerificationUrl(QrEnvironment.TEST, params);

        assertTrue(url.startsWith("https://qr-test.ksef.mf.gov.pl/certificate/" + CONTEXT_TYPE_WIRE
                        + "/" + CONTEXT_VALUE + "/" + SELLER_NIP + "/" + CERTIFICATE_SERIAL + "/"),
                "expected /certificate/ path with positional params, got: " + url);
    }

    @Test
    void buildInvoiceVerificationUrl_whenSellerNipNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> KsefVerificationLinks.buildInvoiceVerificationUrl(
                        QrEnvironment.TEST, null, ISSUE_DATE, new byte[]{1}));
    }

    @Test
    void certificateVerificationParams_whenSignatureNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> new KsefVerificationLinks.CertificateVerificationParams(
                        CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL,
                        new byte[]{1}, null));
    }

    @Test
    void canonicalCertificateSigningPayload_includesQrHostWithoutHttpsPrefix() throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);
        var input = new KsefVerificationLinks.CertificateSigningInput(
                CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL, hash);

        byte[] payload = KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, input);
        String payloadString = new String(payload, java.nio.charset.StandardCharsets.UTF_8);

        // Per kody-qr.md the signed fragment is host + path with https:// stripped:
        // qr-test.ksef.mf.gov.pl/certificate/{contextType}/{contextValue}/{sellerNip}/{certificateSerial}/{hash}
        assertTrue(payloadString.startsWith("qr-test.ksef.mf.gov.pl/certificate/" + CONTEXT_TYPE_WIRE
                        + "/" + CONTEXT_VALUE + "/" + SELLER_NIP + "/" + CERTIFICATE_SERIAL + "/"),
                "expected host+path payload, got: " + payloadString);
        assertFalse(payloadString.startsWith("https://"),
                "payload must omit https:// prefix, got: " + payloadString);
        assertFalse(payloadString.contains("="),
                "base64url segment must not contain padding, got: " + payloadString);
    }

    @Test
    void canonicalCertificateSigningPayload_excludesSignatureSegmentAndMatchesUrlPrefix()
            throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);
        var input = new KsefVerificationLinks.CertificateSigningInput(
                CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL, hash);

        byte[] payload = KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, input);
        var params = new KsefVerificationLinks.CertificateVerificationParams(
                CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL, hash, SIGNATURE);
        String fullUrl = KsefVerificationLinks.buildCertificateVerificationUrl(QrEnvironment.TEST, params);

        // Full URL = "https://" + signing payload + "/" + base64UrlSignature.
        String expectedUrlPrefix = "https://" + new String(payload,
                java.nio.charset.StandardCharsets.UTF_8) + "/";
        assertTrue(fullUrl.startsWith(expectedUrlPrefix),
                "full URL must equal https:// + signing payload + '/' + signature; got: " + fullUrl);
    }

    @Test
    void canonicalCertificateSigningPayload_perEnvironment_picksCorrectQrHost()
            throws NoSuchAlgorithmException {
        byte[] hash = sha256(SAMPLE_INVOICE_XML);
        var input = new KsefVerificationLinks.CertificateSigningInput(
                CONTEXT_TYPE, CONTEXT_VALUE, SELLER_NIP, CERTIFICATE_SERIAL, hash);

        String testPayload = new String(
                KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, input),
                java.nio.charset.StandardCharsets.UTF_8);
        String demoPayload = new String(
                KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.DEMO, input),
                java.nio.charset.StandardCharsets.UTF_8);
        String prodPayload = new String(
                KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.PROD, input),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(testPayload.startsWith("qr-test.ksef.mf.gov.pl/"));
        assertTrue(demoPayload.startsWith("qr-demo.ksef.mf.gov.pl/"));
        assertTrue(prodPayload.startsWith("qr.ksef.mf.gov.pl/"));
    }

    @Test
    void qrEnvironment_eachValue_baseUrlStartsWithHttps() {
        for (QrEnvironment env : QrEnvironment.values()) {
            assertTrue(env.baseUrl().startsWith("https://"),
                    "QR env " + env + " must use https, got: " + env.baseUrl());
        }
    }

    private static byte[] sha256(String input) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
    }
}
