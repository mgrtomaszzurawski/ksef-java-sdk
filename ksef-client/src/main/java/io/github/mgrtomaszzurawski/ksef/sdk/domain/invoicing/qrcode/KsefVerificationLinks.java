/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 */
public final class KsefVerificationLinks {

    private static final DateTimeFormatter DATE_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String INVOICE_PATH = "/invoice/%s/%s/%s";
    private static final String CERTIFICATE_PATH = "/certificate/%s/%s/%s/%s/%s/%s";

    private static final String ERR_NULL_ENVIRONMENT = "environment must not be null";
    private static final String ERR_NULL_SELLER_NIP = "sellerNip must not be null";
    private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
    private static final String ERR_NULL_INVOICE_HASH = "invoiceSha256 must not be null";
    private static final String ERR_NULL_CONTEXT_TYPE = "contextType must not be null";
    private static final String ERR_NULL_CONTEXT_VALUE = "contextValue must not be null";
    private static final String ERR_NULL_CERTIFICATE_SERIAL = "certificateSerial must not be null";
    private static final String ERR_NULL_SIGNATURE = "signature must not be null";

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
                + String.format(INVOICE_PATH, sellerNip, DATE_DD_MM_YYYY.format(issueDate), hashFragment);
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
            String contextType,
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
        Objects.requireNonNull(params, "params must not be null");
        return environment.baseUrl()
                + String.format(CERTIFICATE_PATH,
                        params.contextType(),
                        params.contextValue(),
                        params.sellerNip(),
                        params.certificateSerial(),
                        base64UrlNoPadding(params.invoiceSha256()),
                        base64UrlNoPadding(params.signature()));
    }

    private static String base64UrlNoPadding(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
