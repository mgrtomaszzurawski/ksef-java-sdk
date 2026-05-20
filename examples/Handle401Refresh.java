//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   The SDK transparently re-authenticates when the access token expires —
 *   the consumer doesn't need to wrap calls in try/catch for token refresh.
 *   The snippet forcibly invalidates the session and shows that the next
 *   call still succeeds.
 *
 * Side effects on KSeF:
 *   Performs auth + a read-only call.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP (10 digits)
 *   KSEF_ENV   — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;

public final class Handle401Refresh {

    private Handle401Refresh() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // First authenticated call triggers lazy auth.
            ContextLimits limits1 = client.limits().getContextLimits();
            System.out.println("First call OK — onlineSession.maxInvoiceSizeInMB: "
                    + limits1.onlineSession().maxInvoiceSizeInMB());

            // Force a fresh auth cycle by terminating the current session;
            // the SDK runs lazy auth again on the next authenticated call.
            // In production, the SDK does this transparently in response
            // to a 401 from any authenticated call (HttpRuntime.reauthenticate()).
            client.authSessions().terminate();
            System.out.println("Forced reauth: terminated current session");

            ContextLimits limits2 = client.limits().getContextLimits();
            System.out.println("Second call OK — onlineSession.maxInvoiceSizeInMB: "
                    + limits2.onlineSession().maxInvoiceSizeInMB());

            client.authSessions().terminate();
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
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
