/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;


import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;

/**
 * Build-only access to offline-issuance: assemble an {@link OfflineInvoice}
 * (KOD I + KOD II QR codes, optional technical-correction hash) ready to
 * be sent through an online session. Reached via {@link Invoices#offline()}.
 *
 * <p><strong>This bucket builds; it does not send.</strong> Send the
 * resulting {@link OfflineInvoice} via
 * {@code client.invoices().sessions().online(formCode).sendOfflineInvoice(offline)}.
 * The build / send split matches the offline-issuance flow per
 * {@code ksef-docs/tryby-offline.md}: an invoice is issued (with QR codes)
 * before the session, then transmitted in a subsequent session window.
 *
 * <p>{@link #issue(Invoice, OfflineMode)} uses the
 * {@link OfflineSigningProvider} configured via
 * {@code KsefClient.Builder.offlineSigning(...)} and resolves the
 * authorisation context (QR environment, seller NIP, KOD II context, issue
 * date) from the authenticated client. Use
 * {@link #issue(Invoice, OfflineMode, KsefCertificate)} to override the
 * signing certificate without going through the configured provider — useful
 * for multi-tenant flows where one client signs invoices for several
 * subjects.
 *
 * <p>{@link #issueTechnicalCorrection(Invoice, byte[], OfflineMode)} builds
 * the corrected re-issue per {@code ksef-docs/offline/korekta-techniczna.md}:
 * the resulting {@link OfflineInvoice} carries the original invoice's
 * SHA-256 hash, and the online-session send path routes it through the
 * wire's {@code hashOfCorrectedInvoice} field automatically.
 *
 * @since 1.0.0
 */
public interface OfflineInvoices {

    /**
     * Build a regular (non-correction) {@link OfflineInvoice} using the
     * configured {@link OfflineSigningProvider}. KOD I + KOD II QR codes
     * are computed from the invoice content; the authorisation context
     * (QR environment, seller NIP / context, today's date) is derived
     * from the authenticated {@code KsefClient}.
     *
     * <p><strong>Note on issue date.</strong> The KOD I URL embeds the
     * invoice's calendar issue date. The SDK uses {@code LocalDate.now}
     * in UTC, matching the existing
     * {@code OnlineSession.sendOffline(invoice, mode)} pattern. Per
     * {@code ksef-docs/tryby-offline.md} the canonical issue date is the
     * invoice's {@code P_1} field; with one-day-or-less drift between
     * "now" and {@code P_1} the values coincide for the typical
     * issue-and-send flow.
     *
     * @param invoice the underlying invoice (non-null)
     * @param mode the offline-mode classification (non-null)
     * @return immutable {@link OfflineInvoice} ready for
     *     {@code sendOfflineInvoice}
     * @throws IllegalStateException when the client was built without an
     *     {@link OfflineSigningProvider} or without seller-NIP-bearing
     *     credentials
     */
    <I extends Invoice> OfflineInvoice<I> issue(I invoice, OfflineMode mode);

    /**
     * Build a regular (non-correction) {@link OfflineInvoice} with an
     * explicit signing certificate, bypassing the configured
     * {@link OfflineSigningProvider}. Use this when the cert that signs
     * KOD II is not the one registered with the client — e.g. a
     * multi-tenant flow where each invoice is signed in a different
     * tenant's name.
     *
     * @param invoice the underlying invoice (non-null)
     * @param mode the offline-mode classification (non-null)
     * @param certificate the KSeF Offline certificate to sign KOD II
     *     with (non-null)
     * @return immutable {@link OfflineInvoice}
     * @throws IllegalStateException when the client was built without
     *     environment + seller-NIP context (needed for the KOD I URL)
     */
    <I extends Invoice> OfflineInvoice<I> issue(I invoice, OfflineMode mode, KsefCertificate certificate);

    /**
     * Build a technical-correction {@link OfflineInvoice} carrying the
     * SHA-256 hash of the original (rejected) invoice. Per
     * {@code ksef-docs/offline/korekta-techniczna.md}, the wire request
     * needs both {@code offlineMode=true} AND
     * {@code hashOfCorrectedInvoice}; the online-session
     * {@code sendOfflineInvoice} path inspects the hash on this
     * {@link OfflineInvoice} and routes the send through the
     * technical-correction wire shape automatically.
     *
     * <p>Uses the configured {@link OfflineSigningProvider} (same
     * constraints as {@link #issue(Invoice, OfflineMode)}).
     *
     * @param invoice the corrected (replacement) invoice (non-null)
     * @param hashOfOriginal SHA-256 hash of the original rejected
     *     invoice's XML content (32 bytes, non-null)
     * @param mode the offline-mode classification (non-null)
     * @return immutable {@link OfflineInvoice} with
     *     {@code hashOfCorrectedInvoice} set
     * @throws IllegalArgumentException if {@code hashOfOriginal} is not
     *     32 bytes
     * @throws IllegalStateException when the client was built without an
     *     {@link OfflineSigningProvider} or without seller-NIP-bearing
     *     credentials
     */
    <I extends Invoice> OfflineInvoice<I> issueTechnicalCorrection(I invoice,
                                                                    byte[] hashOfOriginal,
                                                                    OfflineMode mode);
}
