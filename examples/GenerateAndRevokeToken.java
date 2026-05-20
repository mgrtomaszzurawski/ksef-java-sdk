//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Generate a KSeF authentication token, poll until Active, then
 *   revoke it. The token value is shown only once on generate (per
 *   ksef-docs/tokeny-ksef.md); the example does not persist it but
 *   demonstrates the full lifecycle.
 *
 * Side effects on KSeF:
 *   Creates a real token in the authenticated context. Revokes it
 *   at the end. Both create/revoke are non-trivial operations
 *   counted against generate rate limits.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token (for the SDK to authenticate)
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.GenerateTokenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenDetail;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GenerateAndRevokeToken {

    private static final long POLL_INTERVAL_SECONDS = 2L;
    private static final long POLL_BUDGET_SECONDS = 60L;
    private static final String TOKEN_DESCRIPTION = "ksef-sdk demo token";

    private GenerateAndRevokeToken() { }

    public static void main(String[] args) throws Exception {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Generate — returns immediately with reference + raw token
            // value (the value is shown only once).
            TokenGenerateRequest request = new TokenGenerateRequest(
                    TOKEN_DESCRIPTION,
                    List.of(TokenPermissionType.INVOICE_READ));
            GenerateTokenResult generated = client.tokens().generate(request);
            System.out.println("Generated token ref=" + generated.referenceNumber());

            // Poll until Active or budget exhausted. Token starts Pending
            // then transitions asynchronously after KSeF activates the
            // requested permissions.
            TokenStatus status = pollUntilTerminal(client, generated.referenceNumber());
            System.out.println("Token status: " + status);

            // Revoke. Transitions Revoking -> Revoked.
            client.tokens().revoke(generated.referenceNumber());
            System.out.println("Revoke requested");
        }
    }

    private static TokenStatus pollUntilTerminal(KsefClient client, String referenceNumber)
            throws InterruptedException {
        long deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(POLL_BUDGET_SECONDS);
        while (System.currentTimeMillis() < deadlineMs) {
            TokenDetail detail = client.tokens().getStatus(referenceNumber);
            if (detail.status() != TokenStatus.PENDING) {
                return detail.status();
            }
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
        }
        throw new IllegalStateException("Token did not reach terminal state within "
                + POLL_BUDGET_SECONDS + "s");
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
