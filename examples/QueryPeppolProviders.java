//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Query Peppol service providers registered in KSeF. Two flows:
 *     1. Single page via queryProviders(req) with explicit
 *        pageOffset+pageSize from PeppolProvidersQueryRequest.
 *     2. Lazy full traversal via streamProviders() (no-arg —
 *        Peppol endpoint has no filters per spec).
 *
 * Side effects on KSeF:
 *   Read-only (GET /peppol/query).
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import java.util.stream.Stream;

public final class QueryPeppolProviders {

    private static final int PAGE_SIZE = 50;
    private static final int STREAM_PREVIEW_LIMIT = 20;

    private QueryPeppolProviders() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Explicit pagination: walk forward via hasMore + pageOffset++.
            int pageOffset = 0;
            int totalSeen = 0;
            PeppolProvidersResult page;
            do {
                page = client.peppol().queryProviders(
                        new PeppolProvidersQueryRequest(pageOffset, PAGE_SIZE));
                System.out.println("Page " + pageOffset + ": "
                        + page.providers().size() + " providers, hasMore=" + page.hasMore());
                totalSeen += page.providers().size();
                pageOffset++;
            } while (page.hasMore());
            System.out.println("Explicit pagination total: " + totalSeen);

            // Lazy stream: paginator handles offset arithmetic internally;
            // no filter args because the Peppol endpoint has none per spec.
            try (Stream<PeppolProvider> stream = client.peppol().streamProviders()) {
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
