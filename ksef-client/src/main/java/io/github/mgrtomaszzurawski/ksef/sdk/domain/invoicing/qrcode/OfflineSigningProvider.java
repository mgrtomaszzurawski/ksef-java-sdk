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
 * <p>Two default impls are bundled:
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
@FunctionalInterface
public interface OfflineSigningProvider {

    /**
     * Sign and package the invoice into a self-contained
     * {@link OfflineInvoice} carrying KOD I + KOD II PNG bytes alongside
     * the offline-mode classification.
     *
     * @param invoice the underlying invoice (non-null)
     * @param mode the offline-mode classification (non-null)
     * @param context KOD I + KOD II authorisation context bundle (non-null)
     * @return self-contained {@link OfflineInvoice}
     */
    OfflineInvoice signAndPackage(Invoice invoice, OfflineMode mode, OfflineSigningContext context);

    /**
     * In-process provider: signs with the private key already held by
     * {@link KsefCertificate}. Suitable for soft-keystore deployments;
     * for hardware-backed signing implement the interface directly.
     */
    static OfflineSigningProvider fromPrivateKey(KsefCertificate certificate) {
        Objects.requireNonNull(certificate, "certificate must not be null");
        return (invoice, mode, context) ->
                OfflineInvoice.fromInvoice(invoice, certificate, mode, context);
    }
}
