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
 *   Incremental invoice sync — pull every newly-permanently-stored invoice
 *   for a NIP since the last run as a lazy Stream<DecryptedInvoice>. Each
 *   element advances the persisted HWM checkpoint atomically, and the
 *   checkpoint survives JVM restarts via a file-backed CheckpointStore
 *   below.
 *
 * Side effects on KSeF:
 *   Read-only (queryInvoicesByMetadata + export). Writes decrypted XMLs to
 *   the local output directory, plus one JSON checkpoint file per subject
 *   type.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN     — pre-issued KSeF token
 *   KSEF_NIP       — taxpayer NIP (10 digits)
 *   KSEF_ENV       — TEST | DEMO | PROD (optional, default: TEST)
 *   KSEF_OUT_DIR   — output directory for decrypted XMLs + the on-disk
 *                    checkpoint files (optional, default: ./ksef-sync)
 *
 * Sync semantics (see ksef-docs/przyrostowe-pobieranie-faktury.md):
 *   - cursor = permanentStorageHwmDate (PermanentStorage axis)
 *   - dateType is locked to PERMANENT_STORAGE per IncrementalSyncPlan
 *   - consumer may observe the same KsefNumber more than once across runs
 *     (process restarts, overlapping windows) — downstream stores MUST
 *     persist by KsefNumber idempotently.
 *
 * Typed document dispatch:
 *   - DecryptedInvoice.document() carries the typed wrapper —
 *     Fa3InvoiceDocument / Fa2InvoiceDocument / Pef(Kor)InvoiceDocument
 *     for built-in schemas, UnrecognizedInvoiceDocument for unknown forms.
 *   - Register custom InvoiceDocument types via
 *     KsefClient.Builder.invoiceTypes(KsefInvoiceTypes) if your stack
 *     issues invoices under non-KSeF schemas.
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.PefKorInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.UnrecognizedInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.CheckpointStore;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.SyncCheckpoint;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

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
            case "PROD" -> KsefEnvironment.PROD;
            default -> throw new IllegalArgumentException("Unknown KSEF_ENV: " + envName);
        };

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Authentication is lazy — the first authenticated read triggers it.
            System.out.println("Connecting as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            // File-backed CheckpointStore — persists one JSON file per subject
            // type into outDir. Across JVM restarts the next run resumes from
            // the cursor written by the previous run; an unwritten subject type
            // returns Optional.empty() so the first run starts from
            // IncrementalSyncPlan.from(...).
            CheckpointStore checkpointStore = new FileCheckpointStore(outDir);

            // First-time run: start from 30 days ago. Subsequent runs use
            // the persisted checkpoint, ignoring the `from(...)` value.
            OffsetDateTime fromDate = OffsetDateTime.now().minusDays(30);

            IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                    .from(fromDate)
                    .subjectTypes(InvoiceQuerySubjectType.SUBJECT1, InvoiceQuerySubjectType.SUBJECT2)
                    .outputDirectory(outDir)
                    .fullContent(true)
                    .build();

            // Stream lazily yields each accepted invoice. try-with-resources
            // commits the final checkpoint and releases the underlying
            // paginator. The same KsefNumber may arrive twice across runs —
            // downstream must upsert idempotently.
            long processed;
            try (Stream<DecryptedInvoice> stream = client.invoices().sync().asStream(plan, checkpointStore)) {
                processed = stream
                        .peek(IncrementalSync::process)
                        .count();
            }

            System.out.println("Sync done: " + processed + " invoices");
        }
    }

    /**
     * Pattern-match on the typed document slot. For built-in schemas the
     * SDK hands back a concrete typed wrapper; an unknown form lands on
     * {@link UnrecognizedInvoiceDocument} so the {@code default} arm is
     * effectively unreachable today.
     */
    private static void process(DecryptedInvoice decrypted) {
        String ksefRef = decrypted.ksefNumber().value();
        var document = decrypted.document();
        if (document instanceof Fa3InvoiceDocument fa3) {
            System.out.println("FA(3) " + ksefRef + " — " + fa3.unsafeJaxbView().getFa().getP1());
        } else if (document instanceof Fa2InvoiceDocument fa2) {
            System.out.println("FA(2) " + ksefRef + " — " + fa2.unsafeJaxbView().getFa().getP1());
        } else if (document instanceof PefInvoiceDocument pef) {
            System.out.println("PEF " + ksefRef + " — invoiceNumber=" + pef.invoiceNumber());
        } else if (document instanceof PefKorInvoiceDocument pefKor) {
            System.out.println("PEFKOR " + ksefRef + " — invoiceNumber=" + pefKor.invoiceNumber());
        } else if (document instanceof UnrecognizedInvoiceDocument unknown) {
            System.out.println("Unknown form " + unknown.formCode() + " for " + ksefRef
                    + " — " + unknown.xml().length + " bytes raw");
        } else {
            System.out.println("Unhandled document type for " + ksefRef);
        }
    }

    /**
     * File-backed {@link CheckpointStore} writing one text file per
     * subject type (e.g. {@code <outDir>/checkpoint-SUBJECT1.txt}) in
     * a minimal two-line format: ISO-8601 cursor + boolean
     * lastTruncated. Production code typically uses a transactional
     * database; this implementation is a minimal cron-style sync
     * fixture that avoids any extra dependencies beyond ksef-client.
     */
    private static final class FileCheckpointStore implements CheckpointStore {

        private static final String FILE_NAME_PREFIX = "checkpoint-";
        private static final String FILE_NAME_SUFFIX = ".txt";

        private final Path directory;

        FileCheckpointStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public Optional<SyncCheckpoint> load(InvoiceQuerySubjectType subjectType) {
            Path file = fileFor(subjectType);
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                var lines = Files.readAllLines(file);
                if (lines.size() < 2) {
                    return Optional.empty();
                }
                OffsetDateTime cursor = OffsetDateTime.parse(lines.get(0));
                boolean lastTruncated = Boolean.parseBoolean(lines.get(1));
                return Optional.of(new SyncCheckpoint(cursor, lastTruncated));
            } catch (IOException ioFailure) {
                throw new UncheckedIOException("Failed to read checkpoint at " + file, ioFailure);
            }
        }

        @Override
        public void save(InvoiceQuerySubjectType subjectType, SyncCheckpoint checkpoint) {
            Path file = fileFor(subjectType);
            try {
                Files.createDirectories(directory);
                Files.writeString(file,
                        checkpoint.cursor().toString() + System.lineSeparator()
                                + checkpoint.lastTruncated() + System.lineSeparator());
            } catch (IOException ioFailure) {
                throw new UncheckedIOException("Failed to write checkpoint at " + file, ioFailure);
            }
        }

        private Path fileFor(InvoiceQuerySubjectType subjectType) {
            return directory.resolve(FILE_NAME_PREFIX + subjectType.name() + FILE_NAME_SUFFIX);
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
