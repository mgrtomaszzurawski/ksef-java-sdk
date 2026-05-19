//DEPS io.github.mgrtomaszzurawski.ksef-sdk:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Asynchronous bulk export of invoice archive. Submit an export
 *   request, await terminal status via the prepared-export handle,
 *   then stream-decrypt the package to a local directory. The SDK
 *   retains AES key + IV inside the PreparedInvoiceExport handle —
 *   downloadAndDecryptTo() decrypts straight from the wire response
 *   without buffering the full encrypted blob in memory.
 *
 * Side effects on KSeF:
 *   Read-only (POST /invoices/exports/async). Creates a server-side
 *   export job whose lifetime is bounded by KSeF; cancel by closing
 *   the handle if you no longer need it.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_OUT   — output directory for decrypted XMLs (default ./export)
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

public final class ExportInvoiceArchive {

    private ExportInvoiceArchive() { }

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path outDir = Paths.get(System.getenv().getOrDefault("KSEF_OUT", "./export"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        Files.createDirectories(outDir);

        InvoiceQueryRequest query = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(OffsetDateTime.now().minusDays(7))
                .build();

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build();
                // try-with-resources: handle releases AES material on close.
                PreparedInvoiceExport prepared = client.invoices().export()
                        .prepare(query, ExportScope.FULL_CONTENT)) {

            // Blocks until server reports terminal export state.
            InvoiceExportStatus status = prepared.awaitReady();
            System.out.println("Export ready: " + status.status());
            if (status.invoicePackage() != null) {
                System.out.println("Package: "
                        + status.invoicePackage().invoiceCount() + " invoices");
            }

            // Stream-download + decrypt straight to outDir. Memory bounded
            // by chunk buffer, NOT by full package size (KSeF export
            // packages can be multi-GB).
            var dir = prepared.downloadAndDecryptTo(status, outDir);
            System.out.println("Decrypted to " + dir.outputDirectory()
                    + " (" + dir.invoiceXmls().size() + " files)");
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
