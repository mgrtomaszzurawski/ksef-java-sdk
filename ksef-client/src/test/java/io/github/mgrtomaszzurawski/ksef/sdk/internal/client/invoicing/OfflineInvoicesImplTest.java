/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.offline.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the build-only offline-issuance facade: provider-driven happy path,
 * explicit-certificate bypass path, technical-correction wiring, and the
 * documented {@link IllegalStateException} paths (no provider configured;
 * no environment/seller-NIP context).
 *
 * <p>The facade composes — it does not own — the QR rendering and
 * signing flows. These tests deliberately keep that contract narrow:
 * verify the provider is invoked when expected, verify
 * {@code hashOfCorrectedInvoice} is wired through on the correction path,
 * and verify the misconfiguration cases fail loud at the API boundary.
 */
class OfflineInvoicesImplTest {

    private static final String SELLER_NIP = "1111111111";
    private static final int SHA256_LENGTH_BYTES = 32;

    private static KsefCertificate certificate;

    @BeforeAll
    static void initCertificate() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        certificate = new KsefCertificate(pair.certificate(), pair.privateKey());
    }

    @Test
    void issue_whenProviderConfigured_routesThroughProviderWithResolvedContext() {
        CountingProvider provider = new CountingProvider();
        OfflineInvoices facade = new OfflineInvoicesImpl(provider, KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();

        OfflineInvoice<Invoice> result = facade.issue(invoice, OfflineMode.OFFLINE_24);

        assertEquals(1, provider.callCount.get(), "Provider must be invoked exactly once.");
        assertSame(invoice, result.underlyingInvoice());
        assertEquals(OfflineMode.OFFLINE_24, result.offlineMode());
        assertFalse(result.hashOfCorrectedInvoice().isPresent(),
                "Regular (non-correction) issuance must not carry hashOfCorrectedInvoice.");
    }

    @Test
    void issue_whenNoProviderConfigured_throwsIllegalStateException() {
        OfflineInvoices facade = new OfflineInvoicesImpl(null, KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> facade.issue(invoice, OfflineMode.OFFLINE_24));
        assertTrue(thrown.getMessage().contains("OfflineSigningProvider"),
                () -> "Error must point at the missing provider: " + thrown.getMessage());
    }

    @Test
    void issueWithCertificate_whenSupplied_bypassesProvider() {
        CountingProvider provider = new CountingProvider();
        OfflineInvoices facade = new OfflineInvoicesImpl(provider, KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();

        OfflineInvoice<Invoice> result = facade.issue(invoice, OfflineMode.OFFLINE_24, certificate);

        assertEquals(0, provider.callCount.get(),
                "Provider must be bypassed when the certificate is supplied per call.");
        assertSame(invoice, result.underlyingInvoice());
        assertEquals(OfflineMode.OFFLINE_24, result.offlineMode());
        assertSame(certificate, result.signingCertificate());
    }

    @Test
    void issueWithCertificate_whenNoEnvironment_throwsIllegalStateException() {
        OfflineInvoices facade = new OfflineInvoicesImpl(null, null, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> facade.issue(invoice, OfflineMode.OFFLINE_24, certificate));
        assertTrue(thrown.getMessage().contains("environment"),
                () -> "Error must reference missing context: " + thrown.getMessage());
    }

    @Test
    void issueTechnicalCorrection_carriesHashOfCorrectedInvoice() {
        CountingProvider provider = new CountingProvider();
        OfflineInvoices facade = new OfflineInvoicesImpl(provider, KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] originalHash = fillBytes(SHA256_LENGTH_BYTES);

        OfflineInvoice<Invoice> result = facade.issueTechnicalCorrection(
                invoice, originalHash, OfflineMode.OFFLINE_24);

        assertEquals(1, provider.callCount.get(),
                "Technical correction must invoke the provider once to build the base.");
        assertTrue(result.hashOfCorrectedInvoice().isPresent(),
                "Technical correction must carry hashOfCorrectedInvoice on the resulting OfflineInvoice.");
        assertArrayEquals(originalHash, result.hashOfCorrectedInvoice().orElseThrow());
    }

    @Test
    void issueTechnicalCorrection_whenNoProvider_throwsIllegalStateException() {
        OfflineInvoices facade = new OfflineInvoicesImpl(null, KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] originalHash = fillBytes(SHA256_LENGTH_BYTES);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> facade.issueTechnicalCorrection(invoice, originalHash, OfflineMode.OFFLINE_24));
        assertTrue(thrown.getMessage().contains("OfflineSigningProvider"),
                () -> "Error must reference the missing provider: " + thrown.getMessage());
    }

    @Test
    void issue_whenInvoiceNull_throwsNullPointer() {
        OfflineInvoices facade = new OfflineInvoicesImpl(new CountingProvider(),
                KsefEnvironment.TEST, SELLER_NIP);
        assertThrows(NullPointerException.class,
                () -> facade.issue(null, OfflineMode.OFFLINE_24));
    }

    @Test
    void issue_whenModeNull_throwsNullPointer() {
        OfflineInvoices facade = new OfflineInvoicesImpl(new CountingProvider(),
                KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        assertThrows(NullPointerException.class,
                () -> facade.issue(invoice, null));
    }

    @Test
    void issueWithCertificate_whenCertificateNull_throwsNullPointer() {
        OfflineInvoices facade = new OfflineInvoicesImpl(new CountingProvider(),
                KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        assertThrows(NullPointerException.class,
                () -> facade.issue(invoice, OfflineMode.OFFLINE_24, null));
    }

    @Test
    void issueTechnicalCorrection_whenHashNull_throwsNullPointer() {
        OfflineInvoices facade = new OfflineInvoicesImpl(new CountingProvider(),
                KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        assertThrows(NullPointerException.class,
                () -> facade.issueTechnicalCorrection(invoice, null, OfflineMode.OFFLINE_24));
    }

    @Test
    void issueTechnicalCorrection_whenModeNull_throwsNullPointer() {
        OfflineInvoices facade = new OfflineInvoicesImpl(new CountingProvider(),
                KsefEnvironment.TEST, SELLER_NIP);
        Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] hash = fillBytes(SHA256_LENGTH_BYTES);
        assertThrows(NullPointerException.class,
                () -> facade.issueTechnicalCorrection(invoice, hash, null));
    }

    private static byte[] fillBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private static final class CountingProvider implements OfflineSigningProvider {
        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public <I extends Invoice> OfflineInvoice<I> signAndPackage(
                I invoice, OfflineMode mode, OfflineSigningContext context) {
            callCount.incrementAndGet();
            return OfflineInvoice.fromInvoice(invoice, certificate, mode, context);
        }
    }
}
