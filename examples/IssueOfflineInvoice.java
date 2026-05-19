//DEPS io.github.mgrtomaszzurawski.ksef-sdk:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
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
 * Requires:
 *   The KsefClient must be built with an OfflineSigningProvider for
 *   the default issue(invoice, mode) path. Alternative:
 *   issue(invoice, mode, KsefCertificate) bypasses the configured
 *   provider for a single call.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP
 *   KSEF_INVOICE_XML  — path to FA(3) invoice XML
 *   KSEF_CERT_P12     — path to PKCS#12 holding the Offline cert
 *   KSEF_CERT_PASS    — PKCS#12 password
 *   KSEF_ENV          — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
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
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        Path p12Path = Path.of(requireEnv("KSEF_CERT_P12"));
        char[] p12Pass = requireEnv("KSEF_CERT_PASS").toCharArray();
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        Invoice invoice = Invoice.fromXml(FormCode.FA3, invoiceXml);

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefPkcs12Credentials(p12Path, p12Pass, nip))
                // Provider supplies the Offline cert that signs KOD II. The SDK
                // does not see private-key material; the provider owns it.
                .offlineSigning(loadProvider(client -> null))
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
        } finally {
            java.util.Arrays.fill(p12Pass, '\0');
        }
    }

    /** Production: build the provider via KsefCertificate.fromPem(...) or
     *  an HSM/KMS adapter. The placeholder below would be replaced with
     *  the real cert source. */
    @SuppressWarnings("unused")
    private static OfflineSigningProvider loadProvider(java.util.function.Function<?, KsefCertificate> certSource) {
        return OfflineSigningProvider.fromPrivateKey(/* KsefCertificate */ null);
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
