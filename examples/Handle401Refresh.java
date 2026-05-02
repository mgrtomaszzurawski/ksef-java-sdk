/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: demonstrate the SDK's automatic re-authentication on HTTP 401.
 *
 * The SDK transparently re-authenticates when the access token expires —
 * the consumer doesn't need to wrap calls in try/catch for token refresh.
 * This example forcibly invalidates the session and shows that the next
 * call still succeeds.
 *
 * Required env vars:
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP (10 digits)
 *
 * Optional:
 *   KSEF_ENV   — TEST | DEMO | PREPROD | PROD (default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;

public final class Handle401Refresh {

    private Handle401Refresh() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();
            System.out.println("Initial auth complete");

            ContextLimits limits1 = client.limits().getContextLimits();
            System.out.println("First call OK — onlineSession.maxInvoiceSizeInMB: "
                    + limits1.onlineSession().maxInvoiceSizeInMB());

            // Force a fresh auth to demonstrate the round-trip. In production
            // the SDK does this transparently in response to a 401 from any
            // authenticated call (HttpRuntime.reauthenticate()).
            client.reauthenticate();
            System.out.println("Forced reauthenticate() complete");

            ContextLimits limits2 = client.limits().getContextLimits();
            System.out.println("Second call OK — onlineSession.maxInvoiceSizeInMB: "
                    + limits2.onlineSession().maxInvoiceSizeInMB());

            client.terminateAuth();
            System.out.println("Auth session terminated");
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
