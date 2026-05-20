/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.OfflineInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrSigningService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * An invoice issued during an offline window — wraps the underlying
 * {@link Invoice}, carries the offline-mode classification, and exposes
 * the two QR codes (KOD I and KOD II) that the buyer-facing
 * visualisation must show until KSeF assigns the canonical KSeF number.
 *
 * <p>Built via {@link OfflineInvoiceBuilder} (recommended) or
 * {@link #fromInvoice(Invoice, KsefCertificate, OfflineMode, KsefEnvironment, QrContextType, String, String)}
 * (one-shot factory). The {@code offlineMode=true} marker is a
 * transport-level field on {@code SendInvoiceRequest}, not an element
 * inside the invoice XML — so the XML returned by {@link #xml()} is
 * identical to the underlying {@link Invoice#xml()};
 * {@link OnlineSession#sendOfflineInvoice(OfflineInvoice)} sets the
 * wire marker when posting.
 *
 * <p><strong>Immutability and defensive copies.</strong> The XML, KOD I
 * PNG, KOD II PNG, and (optional) {@code hashOfCorrectedInvoice} are
 * defensive-copied on construction and on every accessor call. Equality
 * uses {@link Arrays#equals(byte[], byte[])} on the byte[] fields and
 * {@link Objects#equals(Object, Object)} on the rest.
 *
 * <p>Spec citation: REQ-OFFLINE-001 through REQ-OFFLINE-007;
 * {@code ksef-docs/offline/automatyczne-okreslanie-trybu-offline.md},
 * {@code ksef-docs/offline/awaria-i-niedostepnosc.md},
 * {@code ksef-docs/kody-qr.md}.
 *
 * @param <I> the static {@link Invoice} subtype wrapped by this offline
 *     handle; preserved so {@link #underlyingInvoice()} returns the
 *     typed invoice (e.g. {@code Fa3Invoice}) without downcast
 *
 * @since 0.1.0
 */
public final class OfflineInvoice<I extends Invoice> {

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String ERR_SHA256_UNAVAILABLE = "SHA-256 not available";
    private static final String ERR_NULL_INVOICE = "underlyingInvoice must not be null";
    private static final String ERR_NULL_XML = "xml must not be null";
    private static final String ERR_NULL_KOD_I = "kodIQrPng must not be null";
    private static final String ERR_NULL_KOD_II = "kodIIQrPng must not be null";
    private static final String ERR_NULL_OFFLINE_MODE = "offlineMode must not be null";
    private static final String ERR_NULL_CERTIFICATE = "signingCertificate must not be null";
    private static final String ERR_NULL_CONTEXT = "context must not be null";
    private static final String BYTES_LABEL = " bytes";

    private final I underlyingInvoice;
    private final byte[] xml;
    private final byte[] kodIQrPng;
    private final byte[] kodIIQrPng;
    private final OfflineMode offlineMode;
    private final KsefCertificate signingCertificate;
    private final byte @Nullable [] hashOfCorrectedInvoice;

    public OfflineInvoice(I underlyingInvoice, byte[] xml,
                          byte[] kodIQrPng, byte[] kodIIQrPng,
                          OfflineMode offlineMode, KsefCertificate signingCertificate,
                          byte @Nullable [] hashOfCorrectedInvoice) {
        this.underlyingInvoice = Objects.requireNonNull(underlyingInvoice, ERR_NULL_INVOICE);
        this.xml = Objects.requireNonNull(xml, ERR_NULL_XML).clone();
        this.kodIQrPng = Objects.requireNonNull(kodIQrPng, ERR_NULL_KOD_I).clone();
        this.kodIIQrPng = Objects.requireNonNull(kodIIQrPng, ERR_NULL_KOD_II).clone();
        this.offlineMode = Objects.requireNonNull(offlineMode, ERR_NULL_OFFLINE_MODE);
        this.signingCertificate = Objects.requireNonNull(signingCertificate, ERR_NULL_CERTIFICATE);
        this.hashOfCorrectedInvoice = hashOfCorrectedInvoice == null
                ? null
                : hashOfCorrectedInvoice.clone();
    }

    /**
     * The wrapped {@link Invoice}. Consumers keep access to typed
     * accessors (e.g. {@code Fa3Invoice.unsafeJaxbView()},
     * {@code PefInvoice.invoice()}) through this handle.
     */
    public I underlyingInvoice() {
        return underlyingInvoice;
    }

    /**
     * Invoice XML bytes. Identical to
     * {@link Invoice#xml()} on the underlying invoice — the
     * {@code offlineMode=true} marker is set at the wire layer by
     * {@link OnlineSession#sendOfflineInvoice(OfflineInvoice)} (it is
     * not an element inside the XML). Returns a fresh defensive copy
     * on every call.
     */
    public byte[] xml() {
        return xml.clone();
    }

    /**
     * KOD I (online-style invoice verification) QR-code PNG bytes.
     * Pre-submit, this carries the verification URL based on the
     * invoice content hash (the buyer can verify the invoice once it
     * is uploaded to KSeF). Returns a fresh defensive copy on every
     * call.
     */
    public byte[] kodIQrPng() {
        return kodIQrPng.clone();
    }

    /**
     * KOD II (offline certificate authenticity) QR-code PNG bytes.
     * Carries the cert-signed authenticity URL embedding the offline
     * KSeF certificate serial and a signature over the canonical
     * payload. Returns a fresh defensive copy on every call.
     */
    public byte[] kodIIQrPng() {
        return kodIIQrPng.clone();
    }

    /** Offline-mode classification (consumer-chosen / unavailability / emergency). */
    public OfflineMode offlineMode() {
        return offlineMode;
    }

    /** The KSeF Offline certificate that signed the KOD II QR payload. */
    public KsefCertificate signingCertificate() {
        return signingCertificate;
    }

    /**
     * Optional SHA-256 hash of the corrected invoice — populated only
     * when this offline invoice is also a technical correction
     * (REQ-OFFLINE-004). Returns a fresh defensive copy on every call.
     */
    public Optional<byte[]> hashOfCorrectedInvoice() {
        return Optional.ofNullable(hashOfCorrectedInvoice).map(byte[]::clone);
    }

    /** Convenience delegate to {@code underlyingInvoice().formCode()}. */
    public FormCode formCode() {
        return underlyingInvoice.formCode();
    }

    /**
     * Lower-level factory: build an {@link OfflineInvoice} for a regular
     * (non-correction) offline issuance. Computes the invoice hash,
     * builds KOD I, signs the canonical KOD II payload with the
     * supplied certificate, and renders both QR codes as PNG bytes.
     *
     * <p><strong>Prefer the higher-level path</strong> — configure an
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider}
     * on {@code KsefClient.Builder.offlineSigning(...)} and call
     * {@code session.sendOffline(invoice, mode)}, which derives the
     * environment / context / seller NIP / issue date from the
     * authenticated client. This factory exists for advanced flows that
     * need a fully-customised {@link OfflineInvoice} (non-NIP context,
     * non-today issue date, HSM signing pipelines that bypass the
     * provider interface).
     *
     * @param invoice the underlying invoice (non-null); the SDK reads
     *     {@link Invoice#xml()} once
     * @param certificate the KSeF Offline certificate that signs KOD II
     *     (non-null)
     * @param mode the offline-mode classification (non-null)
     * @param context KOD I + KOD II authorisation context bundle (non-null)
     * @return immutable {@link OfflineInvoice}
     */
    public static <I extends Invoice> OfflineInvoice<I> fromInvoice(I invoice,
                                              KsefCertificate certificate,
                                              OfflineMode mode,
                                              io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext context) {
        Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
        Objects.requireNonNull(certificate, ERR_NULL_CERTIFICATE);
        Objects.requireNonNull(mode, ERR_NULL_OFFLINE_MODE);
        Objects.requireNonNull(context, ERR_NULL_CONTEXT);

        byte[] invoiceXml = invoice.xml();
        byte[] invoiceHash = computeSha256(invoiceXml);

        byte[] kodIPng = renderKodI(context.environment(), context.sellerNip(),
                context.issueDate(), invoiceHash);
        byte[] kodIIPng = renderKodII(context.environment(), context.contextType(),
                context.contextValue(), context.sellerNip(), certificate, invoiceHash);

        return new OfflineInvoice<>(invoice, invoiceXml, kodIPng, kodIIPng,
                mode, certificate, null);
    }

    public static byte[] renderKodI(QrEnvironment environment, String sellerNip,
                             LocalDate issueDate, byte[] invoiceHash) {
        String url = KsefVerificationLinks.buildInvoiceVerificationUrl(
                environment, sellerNip, issueDate, invoiceHash);
        return new QrCodeService().generateQrCode(url);
    }

    public static byte[] renderKodII(QrEnvironment environment, QrContextType contextType,
                              String contextValue, String sellerNip,
                              KsefCertificate certificate, byte[] invoiceHash) {
        KsefVerificationLinks.CertificateSigningInput signingInput =
                new KsefVerificationLinks.CertificateSigningInput(
                        contextType, contextValue, sellerNip,
                        certificate.serialNumber().value(), invoiceHash);
        String url = new QrSigningService().certificateVerificationUrl(
                environment, signingInput, certificate.privateKey());
        return new QrCodeService().generateQrCode(url);
    }

    public static byte[] computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new IllegalStateException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OfflineInvoice<?> that)) {
            return false;
        }
        return Objects.equals(underlyingInvoice, that.underlyingInvoice)
                && Arrays.equals(xml, that.xml)
                && Arrays.equals(kodIQrPng, that.kodIQrPng)
                && Arrays.equals(kodIIQrPng, that.kodIIQrPng)
                && offlineMode == that.offlineMode
                && Objects.equals(signingCertificate, that.signingCertificate)
                && Arrays.equals(hashOfCorrectedInvoice, that.hashOfCorrectedInvoice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(underlyingInvoice, offlineMode, signingCertificate,
                Arrays.hashCode(xml),
                Arrays.hashCode(kodIQrPng),
                Arrays.hashCode(kodIIQrPng),
                Arrays.hashCode(hashOfCorrectedInvoice));
    }

    @Override
    public String toString() {
        return "OfflineInvoice[formCode=" + underlyingInvoice.formCode()
                + ", offlineMode=" + offlineMode
                + ", xml=" + xml.length + BYTES_LABEL
                + ", kodIQrPng=" + kodIQrPng.length + BYTES_LABEL
                + ", kodIIQrPng=" + kodIIQrPng.length + BYTES_LABEL
                + ", hashOfCorrectedInvoice="
                + (hashOfCorrectedInvoice == null ? "<absent>" : hashOfCorrectedInvoice.length + BYTES_LABEL)
                + "]";
    }
}
