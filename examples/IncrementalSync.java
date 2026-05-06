//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: incremental invoice sync — pull every newly-permanently-stored
 * invoice for a NIP since the last run, decrypt to disk, hand each off to
 * a sink for downstream processing.
 *
 * Required env vars:
 *   KSEF_TOKEN  — pre-issued KSeF token
 *   KSEF_NIP    — taxpayer NIP (10 digits)
 *
 * Optional:
 *   KSEF_ENV       — TEST | DEMO | PREPROD | PROD (default: TEST)
 *   KSEF_OUT_DIR   — directory for decrypted invoice XMLs and the
 *                    on-disk checkpoint file (default: ./ksef-sync)
 *
 * Sync semantics (see ksef-docs/przyrostowe-pobieranie-faktury.md):
 *   - cursor = permanentStorageHwmDate (PermanentStorage axis)
 *   - dateType is locked to PERMANENT_STORAGE per IncrementalSyncPlan
 *   - sink may receive the same KsefNumber more than once across runs
 *     (process restarts, overlapping windows) — implementations MUST
 *     persist by KsefNumber idempotently.
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.InvoiceSyncClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

public final class IncrementalSync {

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        String envName = System.getenv().getOrDefault("KSEF_ENV", "TEST");
        Path outDir = Paths.get(System.getenv().getOrDefault("KSEF_OUT_DIR", "./ksef-sync"));

        Files.createDirectories(outDir);

        KsefEnvironment environment = switch (envName.toUpperCase()) {
            case "TEST" -> KsefEnvironment.TEST;
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PREPROD" -> KsefEnvironment.PREPROD;
            case "PROD" -> KsefEnvironment.PROD;
            default -> throw new IllegalArgumentException("Unknown KSEF_ENV: " + envName);
        };

        try (KsefClient client = KsefClient.builder(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();
            System.out.println("Authenticated as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            // The CheckpointStore persists the HWM cursor between runs.
            // For production, replace with a database-backed implementation;
            // CheckpointStore.inMemory() restarts from scratch each run.
            CheckpointStore checkpointStore = CheckpointStore.inMemory();

            // First-time run: start from 30 days ago. Subsequent runs use
            // the persisted checkpoint, ignoring the `from(...)` value.
            OffsetDateTime fromDate = OffsetDateTime.now().minusDays(30);

            IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                    .from(fromDate)
                    .subjectTypes(InvoiceQuerySubjectType.SUBJECT1, InvoiceQuerySubjectType.SUBJECT2)
                    .outputDirectory(outDir)
                    .fullContent(true)
                    .build();

            // Sink is invoked per invoice. Idempotent by KsefNumber:
            // if you re-run after a restart, the same invoice may arrive
            // again — your downstream store should upsert by KSeF number.
            InvoiceSyncClient sync = new InvoiceSyncClient(
                    client.invoices(),
                    new com.fasterxml.jackson.databind.ObjectMapper());

            SyncResult result = sync.sync(plan, checkpointStore, (ksefNumber, metadata, xmlPath) -> {
                System.out.println("Got " + ksefNumber.value()
                        + " issued=" + metadata.issueDate()
                        + " xml=" + (xmlPath == null ? "<no XML>" : xmlPath));
                // TODO: insert into your downstream store keyed by ksefNumber.
            });

            System.out.println("Sync done: " + result.totalProcessed()
                    + " invoices across " + result.processedCounts().size() + " subject type(s)");
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
