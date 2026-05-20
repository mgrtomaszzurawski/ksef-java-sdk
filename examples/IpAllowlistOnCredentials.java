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
 *   Attach an AuthorizationPolicy (IP allow-list) to credentials. The
 *   policy ships in the auth payload — KSeF then restricts the issued
 *   session token to the listed IPv4 addresses / ranges / CIDR masks.
 *
 *   Use case: NAT'd / load-balanced clients that want a session token
 *   restricted to a known IP pool rather than the challenge's single
 *   reported clientIp.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.policy.AuthorizationPolicy;
import java.util.List;

public final class IpAllowlistOnCredentials {

    private IpAllowlistOnCredentials() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        AuthorizationPolicy ipPolicy = new AuthorizationPolicy(
                List.of("192.0.2.10", "192.0.2.11"),
                List.of("10.0.0.1-10.0.0.254"),
                List.of("192.168.0.0/24"));

        KsefTokenCredentials credentials =
                new KsefTokenCredentials(token, KsefIdentifier.nip(nip), ipPolicy);

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(credentials)
                .build()) {
            // Subsequent KSeF calls are bound to the listed IPv4 addresses /
            // ranges / CIDR masks — issued token cannot be replayed from
            // an IP outside the allow-list.
            System.out.println("Authenticated with IP allow-list policy: " + ipPolicy);
        }
    }

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
            default -> KsefEnvironment.TEST;
        };
    }
}
