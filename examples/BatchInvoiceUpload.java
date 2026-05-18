//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Submit a batch of invoices via the synchronous submitBatch facade (PR11).
 *   The SDK encrypts every invoice with a session AES key, splits the encrypted
 *   ZIP into parts, opens a KSeF batch session, uploads every part, closes the
 *   session, polls until terminal, and downloads UPOs for accepted invoices.
 *   By the time submitBatch returns, the result already carries every UPO.
 *
 * Threading warning:
 *   This method blocks the calling thread for minutes to hours, depending on
 *   batch size and upload bandwidth. KSeF batch can be up to 5 GB. Do not call
 *   from UI threads, HTTP request handlers, or reactive framework dispatch
 *   threads. Wrap with a dedicated executor for async use.
 *
 * Side effects on KSeF:
 *   Files real legally-binding invoices in batch. Do not run against PROD without
 *   understanding the consequences.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_INVOICE_XML  — path to one FA(3) invoice XML
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BatchInvoiceUpload {

    private BatchInvoiceUpload() { }

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        List<Invoice> invoices = List.of(Invoice.fromXml(FormCode.FA3, invoiceXml));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Authentication is lazy — submitBatch triggers it on first call.
            System.out.println("Connecting as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            // Threading warning: submitBatch blocks for minutes to hours.
            // Do not call from UI / HTTP handler / reactive dispatch threads.
            // Wrap with CompletableFuture.supplyAsync(executor) for async use.
            var result = client.invoices().batch().submit(invoices, BatchOptions.defaults());

            System.out.println("Batch session: " + result.sessionRef());
            System.out.println("Submitted: " + result.totalCount() + " invoices");
            System.out.println("Cleared:   " + result.successfulCount());
            System.out.println("Failed:    " + result.failedCount());
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
