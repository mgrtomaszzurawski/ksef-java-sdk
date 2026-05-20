//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Accountancy / ERP integrators commonly need to manage multiple
 *   KsefClient instances at once — one per taxpayer (NIP) — and
 *   introspect their configuration to identify, compare, or log them.
 *
 *   KsefClient.config() returns an immutable KsefClientConfig snapshot
 *   with field-by-field equality + mask-safe toString (credentials are
 *   surfaced via a masked KsefCredentialsDescriptor — never the raw
 *   token / private key).
 *
 *   This snippet builds two clients for two taxpayers, identifies them
 *   by config equality, and logs each via the safe toString.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN_A      — KSeF token for taxpayer A
 *   KSEF_NIP_A        — taxpayer A NIP (10 digits)
 *   KSEF_TOKEN_B      — KSeF token for taxpayer B
 *   KSEF_NIP_B        — taxpayer B NIP (10 digits)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefClientConfig;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiTenantOrchestration {

    private static final Map<String, KsefClient> clientsByNip = new ConcurrentHashMap<>();

    private MultiTenantOrchestration() { }

    public static void main(String[] args) {
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        List<Tenant> tenants = List.of(
                new Tenant(requireEnv("KSEF_NIP_A"), requireEnv("KSEF_TOKEN_A")),
                new Tenant(requireEnv("KSEF_NIP_B"), requireEnv("KSEF_TOKEN_B")));

        try {
            for (Tenant tenant : tenants) {
                KsefClient client = buildClient(environment, tenant);
                clientsByNip.put(tenant.nip(), client);
                KsefClientConfig snapshot = client.config();
                System.out.println("Built client for NIP=" + tenant.nip()
                        + " — config snapshot: " + snapshot);
            }

            // Identify clients without comparing KsefClient instances directly:
            KsefClientConfig configA = clientsByNip.get(tenants.get(0).nip()).config();
            KsefClientConfig configB = clientsByNip.get(tenants.get(1).nip()).config();
            System.out.println("Tenants share environment: "
                    + configA.environment().equals(configB.environment()));
            System.out.println("Tenants share retry policy: "
                    + configA.retryPolicy().equals(configB.retryPolicy()));

        } finally {
            clientsByNip.values().forEach(KsefClient::close);
        }
    }

    private static KsefClient buildClient(KsefEnvironment environment, Tenant tenant) {
        return KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(tenant.token(), tenant.nip()))
                .build();
    }

    private record Tenant(String nip, String token) { }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required");
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String name) {
        if (name == null || name.isBlank() || "TEST".equalsIgnoreCase(name)) {
            return KsefEnvironment.TEST;
        }
        return switch (name.toUpperCase()) {
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            case "PREPROD" -> KsefEnvironment.PREPROD;
            default -> KsefEnvironment.TEST;
        };
    }
}
