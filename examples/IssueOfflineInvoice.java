//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference snippet — adapt to your application. Not directly runnable:
 * loadOfflineCertificate() is the single integration seam you must
 * implement (load from PKCS#12 keystore, HSM/KMS, or implement
 * OfflineSigningProvider directly).
 *
 * What this shows:
 *   Build an offline-issued invoice (KOD I + KOD II QR codes) using
 *   the SDK's offline().issue(...) builder. The result is ready for
 *   either:
 *     1. Print / archive locally (offline24 mode — taxpayer-elected),
 *     2. Subsequent send via session.sendOfflineInvoice(...) when
 *        connectivity returns (server-side late delivery).
 *
 *   Also demonstrates issueTechnicalCorrection(...) — re-issuing an
 *   invoice that KSeF rejected for technical reasons (e.g. schema
 *   mismatch) per ksef-docs/offline/korekta-techniczna.md.
 *
 * Inputs (env vars):
 *   KSEF_NIP          — taxpayer NIP
 *   KSEF_INVOICE_XML  — path to FA(3) invoice XML
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class IssueOfflineInvoice {

    private IssueOfflineInvoice() { }

    public static void main(String[] args) throws Exception {
        String ksefToken = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        Invoice invoice = Invoice.fromXml(FormCode.FA3, invoiceXml);

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(ksefToken, nip))
                // Provider supplies the Offline cert that signs KOD II. The SDK
                // never touches private-key material directly — the provider
                // owns it. Replace loadOfflineCertificate() with your cert source.
                .offlineSigning(OfflineSigningProvider.fromPrivateKey(loadOfflineCertificate()))
                .build()) {

            // Issue an offline24 invoice — taxpayer-elected offline mode
            // (vs offline-niedostępność elected by KSeF outage).
            OfflineInvoice<Invoice> offline = client.invoices().offline()
                    .issue(invoice, OfflineMode.OFFLINE_24);

            System.out.println("KOD I QR: " + offline.kodIQrPng().length + " bytes PNG");
            System.out.println("KOD II QR: " + offline.kodIIQrPng().length + " bytes PNG");
            System.out.println("Mode: " + offline.offlineMode());

            // Later, when connectivity returns, send through an online session:
            //   try (OnlineSession session = client.invoices().sessions().online(FormCode.FA3)) {
            //       SubmittedInvoice<Invoice> result = session.sendOfflineInvoice(offline);
            //   }

            // Technical correction — re-issuing an invoice KSeF previously
            // rejected for technical reasons. hashOfOriginal is SHA-256 of
            // the rejected invoice's XML body.
            byte[] hashOfOriginal = MessageDigest.getInstance("SHA-256").digest(invoiceXml);
            OfflineInvoice<Invoice> correction = client.invoices().offline()
                    .issueTechnicalCorrection(invoice, hashOfOriginal, OfflineMode.OFFLINE_24);
            System.out.println("Correction with hashOfCorrectedInvoice: "
                    + correction.hashOfCorrectedInvoice().isPresent());
        }
    }

    /**
     * Integration seam — supply the KSeF Offline certificate that signs
     * KOD II. Typical sources:
     * <ul>
     *   <li>PKCS#12 keystore on disk — {@code KeyStore.getInstance("PKCS12")}
     *       then extract via {@code keyStore.getKey(alias, pass)} +
     *       {@code keyStore.getCertificate(alias)}.</li>
     *   <li>HSM / KMS — implement {@link OfflineSigningProvider} directly
     *       so the SDK never sees the private-key material.</li>
     * </ul>
     */
    private static KsefCertificate loadOfflineCertificate() {
        throw new UnsupportedOperationException(
                "Replace loadOfflineCertificate() with your KsefCertificate source"
                        + " — see method Javadoc for typical patterns.");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String envName) {
        if (envName == null || envName.isBlank()) {
            return KsefEnvironment.TEST;
        }
        return switch (envName.toUpperCase()) {
            case "TEST" -> KsefEnvironment.TEST;
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
