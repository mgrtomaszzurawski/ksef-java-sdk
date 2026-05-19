//DEPS io.github.mgrtomaszzurawski.ksef-sdk:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   List the consumer's active auth sessions in the current KSeF
 *   context, lazily paginated via x-continuation-token. Demonstrates
 *   the R2-9d shape — streamAuthSessions(filter) honours pageSize
 *   from the filter, paginator manages the cursor internally.
 *
 *   Also demonstrates terminating another session by reference
 *   (e.g. forcing logout of a stale token).
 *
 * Side effects on KSeF:
 *   Stream is read-only. terminateSession(ref) terminates the
 *   referenced session server-side.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionsQueryRequest;
import java.util.stream.Stream;

public final class ListAuthSessions {

    private static final int PAGE_SIZE = 50;
    private static final int PREVIEW_LIMIT = 10;

    private ListAuthSessions() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Drive lazy auth by issuing the stream call (it triggers
            // the challenge handshake before the first GET).
            AuthSessionsQueryRequest filter = AuthSessionsQueryRequest.firstPage(PAGE_SIZE);
            try (Stream<AuthSession> stream = client.authSessions().streamAuthSessions(filter)) {
                stream.limit(PREVIEW_LIMIT)
                        .forEach(session -> System.out.println(
                                session.referenceNumber()
                                        + " current=" + session.current()
                                        + " method=" + session.authenticationMethod()));
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
