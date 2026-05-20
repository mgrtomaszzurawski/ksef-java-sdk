//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Submit a batch of invoices through KSeF's batch flow, wrapped in
 *   CompletableFuture so the caller can do other work while the SDK
 *   uploads, polls, and downloads UPOs in the background. The SDK
 *   facade is synchronous (blocks the calling thread for minutes to
 *   hours per ADR; KSeF batch can be up to 5 GB), so consumer code
 *   that wants async semantics MUST wrap it via an Executor.
 *
 * Pattern demonstrated:
 *   CompletableFuture.supplyAsync(supplier, executor) -> chain
 *   .thenAccept(...) for the result side-effect, .exceptionally(...)
 *   for failure handling. Executor: a small bounded pool, NOT the
 *   common ForkJoinPool (batch I/O would starve CPU-bound tasks).
 *
 * Side effects on KSeF:
 *   Files real legally-binding invoices in batch. Do not run against
 *   PROD without understanding the consequences.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_INVOICE_XML  — path to one FA(3) invoice XML
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class BatchInvoiceUpload {

    /** Hard cap on how long the caller waits for the future. Batch can take
     *  hours per ADR; tune to your operational expectations. */
    private static final long BATCH_AWAIT_HOURS = 2L;

    private BatchInvoiceUpload() { }

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        Path invoicePath = Path.of(requireEnv("KSEF_INVOICE_XML"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        List<Invoice> invoices = List.of(Invoice.fromXml(FormCode.FA3, invoiceXml));

        // Dedicated executor — NOT the common ForkJoinPool. Batch I/O would
        // starve any CPU-bound tasks sharing the pool. One thread is enough
        // here (one batch at a time); raise the size for fan-out workloads.
        ExecutorService batchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ksef-batch-worker");
            thread.setDaemon(true);
            return thread;
        });

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Authentication is lazy — submit triggers it on the worker
            // thread on first call.
            System.out.println("Connecting as ***" + nip.substring(Math.max(0, nip.length() - 4)));
            System.out.println("Dispatching batch (caller thread is free to do other work)...");

            CompletableFuture<BatchResult<Invoice>> future = CompletableFuture
                    .supplyAsync(() -> client.invoices().sessions().batch()
                            .submit(invoices, BatchOptions.defaults()), batchExecutor)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            System.err.println("Batch failed: " + failure.getMessage());
                            return;
                        }
                        System.out.println("Batch session: " + result.sessionRef());
                        System.out.println("Submitted: " + result.totalCount() + " invoices");
                        System.out.println("Cleared:   " + result.successfulCount());
                        System.out.println("Failed:    " + result.failedCount());
                    });

            // Caller can do its own work here while the batch progresses.
            // For the example we just block waiting for completion within a
            // configured deadline.
            try {
                future.get(BATCH_AWAIT_HOURS, TimeUnit.HOURS);
            } catch (TimeoutException timeout) {
                future.cancel(true);
                System.err.println("Batch did not finish within " + BATCH_AWAIT_HOURS + "h — cancelled.");
            }
        } finally {
            batchExecutor.shutdown();
            if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
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
