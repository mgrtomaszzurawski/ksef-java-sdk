//DEPS io.github.mgrtomaszzurawski:ksef-client:0.1.0-preview
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example for the unofficial KSeF SDK (preview).
 * Not affiliated with Ministerstwo Finansow or CIRFMF.
 * API may change between 0.x releases; AGPL-3.0 warranty
 * disclaimer applies. For production use the official SDK:
 * https://github.com/CIRFMF/ksef-client-java
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Open an online KSeF session, send a single invoice, retrieve the UPO.
 *
 * Side effects on KSeF:
 *   Files a real legally-binding invoice. Do not run against PROD without
 *   understanding the consequences.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_INVOICE_XML  — path to a FA(3) invoice XML file
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ClearedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
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

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Authentication is lazy — opening a session triggers it.
            System.out.println("Connecting as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            OnlineSession session = client.invoices().sessions().online(FormCode.FA3);
            System.out.println("Session opened: " + session.referenceNumber());

            // sendInvoice blocks until KSeF reaches a terminal state and
            // returns the full SubmittedInvoice (KSeF number, KOD I QR PNG,
            // status, embedded original Invoice).
            var submitted = session.sendInvoice(Invoice.fromXml(FormCode.FA3, invoiceXml));
            System.out.println("Invoice accepted, ref: " + submitted.referenceNumber()
                    + ", ksefNumber: " + submitted.ksefNumber().map(KsefNumber::value).orElse("<none>"));

            // Transition the session to terminal state (KSeF emits one UPO per
            // accepted invoice once the session is closed) and retrieve the UPO
            // for the invoice we just submitted.
            ClosedSession closed = session.complete();
            var cleared = closed.cleared(submitted);
            System.out.println("UPO bytes for ref " + submitted.referenceNumber()
                    + ": " + cleared.upo().xmlBytes().length);
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
