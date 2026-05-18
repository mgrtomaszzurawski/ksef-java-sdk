/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineMode;
import java.util.Objects;

/**
 * Strategy for producing an {@link OfflineInvoice} from a plain
 * {@link Invoice} during offline session flows. The provider owns the
 * KSeF Offline certificate and the private key that signs KOD II — the
 * SDK does not see private-key material directly.
 *
 * <p>The single method is generic on {@code <I extends Invoice>} so the
 * typed input invoice is preserved on the returned {@link OfflineInvoice}
 * (R1-6). Java does not allow lambdas to target SAMs with a generic
 * method — implement this interface via anonymous class or a named class.
 *
 * <p>One default impl is bundled:
 *
 * <ul>
 *   <li>{@link #fromPrivateKey(KsefCertificate)} — in-process signing.
 *       The {@link KsefCertificate} already carries the private key
 *       reference; the provider simply delegates to
 *       {@link OfflineInvoice#fromInvoice}.</li>
 * </ul>
 *
 * <p>For HSM / KMS-backed signing, implement this interface directly.
 * The provider is configured once at {@code KsefClient.Builder.offlineSigning(...)}
 * and is then consumed by the offline-send flow on each invoice.
 *
 * @since 1.0.0
 */
public interface OfflineSigningProvider {

    /** Internal — null-arg message for the {@link #fromPrivateKey} factory. */
    String ERR_NULL_CERTIFICATE = "certificate must not be null";

    /**
     * Sign and package the invoice into a self-contained
     * {@link OfflineInvoice} carrying KOD I + KOD II PNG bytes alongside
     * the offline-mode classification.
     *
     * @param <I> the static {@link Invoice} subtype passed in; preserved
     *     on the returned {@link OfflineInvoice} so consumer code keeps
     *     typed access to the underlying invoice
     * @param invoice the underlying invoice (non-null)
     * @param mode the offline-mode classification (non-null)
     * @param context KOD I + KOD II authorisation context bundle (non-null)
     * @return self-contained {@link OfflineInvoice}
     */
    <I extends Invoice> OfflineInvoice<I> signAndPackage(I invoice, OfflineMode mode, OfflineSigningContext context);

    /**
     * In-process provider: signs with the private key already held by
     * {@link KsefCertificate}. Suitable for soft-keystore deployments;
     * for hardware-backed signing implement the interface directly.
     */
    static OfflineSigningProvider fromPrivateKey(KsefCertificate certificate) {
        Objects.requireNonNull(certificate, ERR_NULL_CERTIFICATE);
        return new OfflineSigningProvider() {
            @Override
            public <I extends Invoice> OfflineInvoice<I> signAndPackage(
                    I invoice, OfflineMode mode, OfflineSigningContext context) {
                return OfflineInvoice.fromInvoice(invoice, certificate, mode, context);
            }
        };
    }
}
