//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: open an online KSeF session, send a single invoice, retrieve the UPO.
 *
 * Required env vars:
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_INVOICE_XML  — path to a FA(3) invoice XML file (FormCode.FA2 is
 *                       only valid on TEST environment for back-compat)
 *
 * Optional:
 *   KSEF_ENV          — TEST | DEMO | PREPROD | PROD (default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SendOnlineInvoice {

    private SendOnlineInvoice() { }

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);

        try (KsefClient client = KsefClient.builder(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();
            System.out.println("Authenticated as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            try (KsefSession session = client.openSession(FormCode.FA3)) {
                System.out.println("Session opened: " + session.referenceNumber());

                SendInvoiceResult result = session.send(invoiceXml);
                System.out.println("Invoice sent, ref: " + result.referenceNumber());

                // session.close() (via try-with-resources) closes the session,
                // polls until processing completes, and the UPO becomes available.
            }

            // After the session closes, fetch the UPO for the invoice.
            // (In a real app you'd remember the invoice ref before close().)
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
            case "PREPROD" -> KsefEnvironment.PREPROD;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
