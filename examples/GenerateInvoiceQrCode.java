//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Generate a KOD I (invoice verification) QR code from invoice
 *   metadata. KOD I encodes a URL that the buyer can scan to verify
 *   the invoice in KSeF. The example computes SHA-256 of the invoice
 *   XML, builds the QR PNG via the SDK, and writes it to disk.
 *
 *   No authentication required — KOD I generation is fully local
 *   (URL composition + QR rendering). KsefClient is constructed only
 *   to access the qrCode() facade; no wire calls are made.
 *
 * Inputs (env vars):
 *   KSEF_NIP          — seller NIP
 *   KSEF_INVOICE_XML  — path to FA(3) invoice XML
 *   KSEF_QR_OUT       — output PNG path (default ./kod-i.png)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;

public final class GenerateInvoiceQrCode {

    private static final String OFFLINE_LABEL = "OFFLINE";

    private GenerateInvoiceQrCode() { }

    public static void main(String[] args) throws Exception {
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        Path qrOut = Path.of(System.getenv().getOrDefault("KSEF_QR_OUT", "./kod-i.png"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(invoiceXml);

        // Client constructed but not authenticated — qrCode() facade
        // operates fully offline. Token "x" is a placeholder; SDK only
        // requires non-null credentials at build time.
        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials("placeholder-token", nip))
                .build()) {

            QrCodes qrCodes = client.qrCode();
            byte[] kodI = qrCodes.generateKodIQr(
                    QrEnvironment.fromKsefEnvironment(environment),
                    nip,
                    LocalDate.now(),
                    sha256,
                    OFFLINE_LABEL);

            Files.write(qrOut, kodI);
            System.out.println("KOD I written: " + qrOut + " (" + kodI.length + " bytes)");
        }
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
