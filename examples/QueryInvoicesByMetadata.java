//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Query invoice metadata with filters + explicit pagination
 *   (queryByMetadata page-at-a-time) AND lazy full traversal
 *   (streamByMetadata). Post-R2-9a: InvoiceQueryRequest carries
 *   pageOffset/pageSize for explicit navigation; stream ignores
 *   them and walks from page 0 automatically (including server-side
 *   10 000-record truncation handling).
 *
 * Side effects on KSeF:
 *   Read-only (POST /invoices/query/metadata).
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

public final class QueryInvoicesByMetadata {

    private static final int PAGE_SIZE = 100;
    private static final int STREAM_PREVIEW_LIMIT = 25;

    private QueryInvoicesByMetadata() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Explicit pagination — build a fresh request per page with
            // an incremented pageOffset. Suitable for UI grids and
            // bounded fetches where the consumer drives navigation.
            int totalSeen = 0;
            int pageOffset = 0;
            InvoiceMetadataResult page;
            do {
                InvoiceQueryRequest request = InvoiceQueryBuilder.seller()
                        .invoicingDateFrom(OffsetDateTime.now().minusDays(30))
                        .pageSize(PAGE_SIZE)
                        .pageOffset(pageOffset)
                        .build();
                page = client.invoices().archive().queryByMetadata(request);
                System.out.println("Page " + pageOffset + ": " + page.invoices().size()
                        + " invoices, hasMore=" + page.hasMore()
                        + ", isTruncated=" + page.isTruncated());
                totalSeen += page.invoices().size();
                pageOffset++;

                // 10 000-record cap. In production, narrow dateRange
                // by advancing dateFrom past the last record and reset
                // pageOffset=0. Streamed traversal handles this for you.
                if (page.isTruncated()) {
                    System.out.println("Hit isTruncated=true — narrow dateRange and reset pageOffset.");
                    break;
                }
            } while (page.hasMore());
            System.out.println("Explicit pagination total: " + totalSeen + " invoices");

            // Lazy stream — paginator handles offset arithmetic AND
            // truncation-driven dateRange advancement automatically.
            InvoiceQueryRequest streamRequest = InvoiceQueryBuilder.seller()
                    .invoicingDateFrom(OffsetDateTime.now().minusDays(30))
                    .build();
            try (Stream<InvoiceMetadata> stream =
                         client.invoices().archive().streamByMetadata(streamRequest)) {
                long streamed = stream.limit(STREAM_PREVIEW_LIMIT).count();
                System.out.println("Stream preview (first " + STREAM_PREVIEW_LIMIT + "): " + streamed);
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
