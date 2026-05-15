/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Builds KSeF 2.0 verification URLs to be embedded in invoice QR codes.
 *
 * <p>Specified in <a href="https://github.com/CIRFMF/ksef-docs/blob/main/kody-qr.md">kody-qr.md</a>.
 *
 * <ul>
 *   <li><b>KOD I</b> — invoice verification link, used on every KSeF invoice.</li>
 *   <li><b>KOD II</b> — certificate verification link, used on offline-mode invoices
 *       signed with a KSeF Offline certificate. Carries the SHA-256 of the invoice
 *       and a signature over the path so the verifier can confirm the offline
 *       certificate authorised the invoice.</li>
 * </ul>
 *
 * <p>Hashes and signatures are encoded with Base64 URL-safe (no padding).
 *
 * @since 1.0.0
 */
public final class KsefVerificationLinks {

    private static final DateTimeFormatter DATE_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    @SuppressWarnings("java:S1075")
    private static final String INVOICE_PATH = "/invoice/%s/%s/%s";
    @SuppressWarnings("java:S1075")
    private static final String CERTIFICATE_PATH = "/certificate/%s/%s/%s/%s/%s/%s";
    @SuppressWarnings("java:S1075")
    private static final String CERTIFICATE_SIGNING_PAYLOAD = "/certificate/%s/%s/%s/%s/%s";
    private static final String HTTPS_PREFIX = "https://";

    private static final String ERR_NULL_ENVIRONMENT = "environment must not be null";
    private static final String ERR_NULL_SELLER_NIP = "sellerNip must not be null";
    private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
    private static final String ERR_NULL_INVOICE_HASH = "invoiceSha256 must not be null";
    private static final String ERR_NULL_CONTEXT_TYPE = "contextType must not be null";
    private static final String ERR_NULL_CONTEXT_VALUE = "contextValue must not be null";
    private static final String ERR_NULL_CERTIFICATE_SERIAL = "certificateSerial must not be null";
    private static final String ERR_NULL_SIGNATURE = "signature must not be null";
    private static final String ERR_NULL_PARAMS = "params must not be null";

    private KsefVerificationLinks() {
        // Utility class.
    }

    /**
     * Build a KOD I (online) invoice verification URL of the form
     * {@code https://qr-{env}.ksef.mf.gov.pl/invoice/{sellerNip}/{DD-MM-YYYY}/{base64UrlSha256}}.
     *
     * @param environment QR environment (TEST/DEMO/PROD)
     * @param sellerNip 10-digit NIP of the seller (issuer)
     * @param issueDate invoice issue date
     * @param invoiceSha256 32-byte SHA-256 hash of the invoice XML
     * @return verification URL
     */
    public static String buildInvoiceVerificationUrl(QrEnvironment environment,
                                                     String sellerNip,
                                                     LocalDate issueDate,
                                                     byte[] invoiceSha256) {
        Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
        Objects.requireNonNull(sellerNip, ERR_NULL_SELLER_NIP);
        Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE);
        Objects.requireNonNull(invoiceSha256, ERR_NULL_INVOICE_HASH);
        String hashFragment = base64UrlNoPadding(invoiceSha256);
        return environment.baseUrl()
                + String.format(INVOICE_PATH,
                        encodePathSegment(sellerNip),
                        DATE_DD_MM_YYYY.format(issueDate),
                        hashFragment);
    }

    /**
     * Parameters for KOD II offline-certificate verification URL construction.
     *
     * @param contextType authorising context identifier type (e.g. "nip", "internalId")
     * @param contextValue authorising context identifier value
     * @param sellerNip 10-digit NIP of the seller
     * @param certificateSerial KSeF Offline certificate serial number
     * @param invoiceSha256 32-byte SHA-256 hash of the invoice XML
     * @param signature signature bytes over the path components
     */
    public record CertificateVerificationParams(
            QrContextType contextType,
            String contextValue,
            String sellerNip,
            String certificateSerial,
            byte[] invoiceSha256,
            byte[] signature) {

        public CertificateVerificationParams {
            Objects.requireNonNull(contextType, ERR_NULL_CONTEXT_TYPE);
            Objects.requireNonNull(contextValue, ERR_NULL_CONTEXT_VALUE);
            Objects.requireNonNull(sellerNip, ERR_NULL_SELLER_NIP);
            Objects.requireNonNull(certificateSerial, ERR_NULL_CERTIFICATE_SERIAL);
            Objects.requireNonNull(invoiceSha256, ERR_NULL_INVOICE_HASH);
            Objects.requireNonNull(signature, ERR_NULL_SIGNATURE);
            invoiceSha256 = invoiceSha256.clone();
            signature = signature.clone();
        }

        @Override
        public byte[] invoiceSha256() {
            return invoiceSha256.clone();
        }

        @Override
        public byte[] signature() {
            return signature.clone();
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || (o instanceof CertificateVerificationParams other
                        && contextType == other.contextType
                        && Objects.equals(contextValue, other.contextValue)
                        && Objects.equals(sellerNip, other.sellerNip)
                        && Objects.equals(certificateSerial, other.certificateSerial)
                        && Arrays.equals(invoiceSha256, other.invoiceSha256)
                        && Arrays.equals(signature, other.signature));
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextType, contextValue, sellerNip, certificateSerial,
                    Arrays.hashCode(invoiceSha256), Arrays.hashCode(signature));
        }

        @Override
        public String toString() {
            return "CertificateVerificationParams[contextType=" + contextType
                    + ", contextValue=" + contextValue
                    + ", sellerNip=" + sellerNip
                    + ", certificateSerial=" + certificateSerial
                    + ", invoiceSha256=" + invoiceSha256.length + " bytes"
                    + ", signature=" + signature.length + " bytes]";
        }
    }

    /**
     * Build a KOD II (offline-certificate) verification URL of the form
     * {@code https://qr-{env}.ksef.mf.gov.pl/certificate/{contextType}/{contextValue}/{sellerNip}/{certificateSerial}/{base64UrlSha256}/{base64UrlSignature}}.
     *
     * <p>The signature is produced by the seller offline KSeF certificate over the
     * concatenation of the path components preceding the signature segment. The
     * signing material itself is out of scope for this builder — callers pass the
     * already-computed signature bytes.
     *
     * @param environment QR environment (TEST/DEMO/PROD)
     * @param params certificate-verification parameters
     * @return verification URL
     */
    public static String buildCertificateVerificationUrl(QrEnvironment environment,
                                                         CertificateVerificationParams params) {
        Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
        Objects.requireNonNull(params, ERR_NULL_PARAMS);
        return environment.baseUrl()
                + String.format(CERTIFICATE_PATH,
                        encodePathSegment(params.contextType().wireValue()),
                        encodePathSegment(params.contextValue()),
                        encodePathSegment(params.sellerNip()),
                        encodePathSegment(params.certificateSerial()),
                        base64UrlNoPadding(params.invoiceSha256()),
                        base64UrlNoPadding(params.signature()));
    }

    /**
     * Subset of {@link CertificateVerificationParams} that omits the
     * signature, used as input to {@link #canonicalCertificateSigningPayload}
     * before the signature is computed.
     */
    public record CertificateSigningInput(
            QrContextType contextType,
            String contextValue,
            String sellerNip,
            String certificateSerial,
            byte[] invoiceSha256) {

        public CertificateSigningInput {
            Objects.requireNonNull(contextType, ERR_NULL_CONTEXT_TYPE);
            Objects.requireNonNull(contextValue, ERR_NULL_CONTEXT_VALUE);
            Objects.requireNonNull(sellerNip, ERR_NULL_SELLER_NIP);
            Objects.requireNonNull(certificateSerial, ERR_NULL_CERTIFICATE_SERIAL);
            Objects.requireNonNull(invoiceSha256, ERR_NULL_INVOICE_HASH);
            invoiceSha256 = invoiceSha256.clone();
        }

        @Override
        public byte[] invoiceSha256() {
            return invoiceSha256.clone();
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || (o instanceof CertificateSigningInput other
                        && contextType == other.contextType
                        && Objects.equals(contextValue, other.contextValue)
                        && Objects.equals(sellerNip, other.sellerNip)
                        && Objects.equals(certificateSerial, other.certificateSerial)
                        && Arrays.equals(invoiceSha256, other.invoiceSha256));
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextType, contextValue, sellerNip, certificateSerial,
                    Arrays.hashCode(invoiceSha256));
        }

        @Override
        public String toString() {
            return "CertificateSigningInput[contextType=" + contextType
                    + ", contextValue=" + contextValue
                    + ", sellerNip=" + sellerNip
                    + ", certificateSerial=" + certificateSerial
                    + ", invoiceSha256=" + invoiceSha256.length + " bytes]";
        }
    }

    /**
     * Return the canonical signing payload that the offline KSeF certificate
     * private key must sign in order to produce the {@code signature} segment
     * of the KOD II URL.
     *
     * <p>Per the official KSeF QR documentation
     * (<a href="https://github.com/CIRFMF/ksef-docs/blob/main/kody-qr.md">kody-qr.md</a>)
     * the signed fragment is the QR host + path WITHOUT the {@code https://}
     * prefix and WITHOUT the trailing signature segment, encoded as UTF-8 bytes:
     * {@code qr-{env}.ksef.mf.gov.pl/certificate/{contextType}/{contextValue}/{sellerNip}/{certificateSerial}/{base64UrlSha256}}.
     *
     * <p>Callers sign these bytes with their offline KSeF certificate private
     * key. Per {@code kody-qr.md} accepted algorithms are
     * <b>RSASSA-PSS with SHA-256, MGF1-SHA-256, salt length 32</b> or
     * <b>ECDSA on P-256 with SHA-256</b>. Pass the resulting signature into
     * {@link CertificateVerificationParams} and call
     * {@link #buildCertificateVerificationUrl(QrEnvironment, CertificateVerificationParams)}.
     *
     * @param environment QR environment whose host is included in the signed bytes
     * @param input certificate-verification parameters (no signature)
     * @return canonical signing payload bytes (UTF-8 encoded host+path)
     */
    public static byte[] canonicalCertificateSigningPayload(QrEnvironment environment,
                                                            CertificateSigningInput input) {
        Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
        Objects.requireNonNull(input, ERR_NULL_PARAMS);
        String hostWithoutScheme = stripHttpsPrefix(environment.baseUrl());
        String payloadPath = String.format(CERTIFICATE_SIGNING_PAYLOAD,
                encodePathSegment(input.contextType().wireValue()),
                encodePathSegment(input.contextValue()),
                encodePathSegment(input.sellerNip()),
                encodePathSegment(input.certificateSerial()),
                base64UrlNoPadding(input.invoiceSha256()));
        return (hostWithoutScheme + payloadPath).getBytes(StandardCharsets.UTF_8);
    }

    private static String stripHttpsPrefix(String url) {
        return url.startsWith(HTTPS_PREFIX) ? url.substring(HTTPS_PREFIX.length()) : url;
    }

    private static String base64UrlNoPadding(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encodePathSegment(String segment) {
        // URLEncoder is form-encoding; convert '+' back to '%20' for path-segment use.
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
