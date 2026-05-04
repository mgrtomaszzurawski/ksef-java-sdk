//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: open a batch session, upload pre-built parts, close, poll until terminal.
 *
 * Required env vars:
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_INVOICE_XML  — path to one FA(2) invoice XML; the example wraps it in a single-invoice batch
 *
 * Optional:
 *   KSEF_ENV          — TEST | DEMO | PREPROD | PROD (default: TEST)
 *
 * The SDK builds the encrypted ZIP from your raw XMLs, splits it into part files,
 * computes hashes, and gives you the upload URLs. {@code uploadParts()} HTTP-PUTs
 * each part. {@code close()} signals to KSeF that the upload is done and polls
 * status until it's terminal.
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
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
        List<byte[]> invoices = List.of(invoiceXml);

        try (KsefClient client = KsefClient.builder(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();
            System.out.println("Authenticated as " + nip);

            try (KsefBatchSession batch = client.openBatchSession(FormCode.FA2, invoices)) {
                System.out.println("Batch session: " + batch.referenceNumber());
                System.out.println("Parts to upload: " + batch.partUploadRequests().size());

                batch.uploadParts();
                System.out.println("All parts uploaded");

                // try-with-resources close() finalises the batch and polls until terminal.
            }

            // After close, fetch final status if you want a sanity check.
            // (Re-create a client.sessions() call or pass batch.referenceNumber() through to your reporting.)
            System.out.println("Batch processing complete");
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
            case "PREPROD" -> KsefEnvironment.PREPROD;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
