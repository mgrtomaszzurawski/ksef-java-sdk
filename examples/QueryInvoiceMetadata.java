//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Paginate through every invoice metadata entry in a date range. The
 *   SDK's streamInvoicesByMetadata() walks all pages using permanentStorageHwmDate
 *   as a date cursor lazily — pipe through .limit(N).toList() to bound
 *   memory.
 *
 * Side effects on KSeF:
 *   Read-only.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP (10 digits)
 *   KSEF_ENV   — TEST | DEMO | PROD (optional, default: TEST)
 *   KSEF_DAYS  — how many days back to query (optional, default: 30)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import java.time.OffsetDateTime;
import java.util.List;

public final class QueryInvoiceMetadata {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_RESULTS = 5000;

    private QueryInvoiceMetadata() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));
        int days = parseInt(System.getenv("KSEF_DAYS"), DEFAULT_DAYS);

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();

            InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(OffsetDateTime.now().minusDays(days))
                    .dateTo(OffsetDateTime.now());

            List<InvoiceMetadata> all = client.invoices().streamInvoicesByMetadata(query)
                    .limit(MAX_RESULTS)
                    .toList();
            System.out.println("Found " + all.size() + " invoices in the last " + days + " days");

            for (InvoiceMetadata invoice : all) {
                System.out.println("  " + invoice.ksefNumber() + "  "
                        + invoice.invoiceNumber() + "  " + invoice.grossAmount() + " " + invoice.currency());
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

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
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
